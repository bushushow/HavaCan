"""Builds Türkiye province + district coordinates for the app.

Province/district lists come from GeoNames (admin1/admin2 codes, CC BY 4.0); coordinates come
from the Open-Meteo Geocoding API, matched on the same GeoNames ids (admin1_id / admin2_id).
Usage: python build_places.py <geonames_dir> <out_json>
"""
import json
import sys
import time
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor

GEO_DIR, OUT = sys.argv[1], sys.argv[2]
API = "https://geocoding-api.open-meteo.com/v1/search"
# Province capitals whose name differs from the province
CAPITALS = {"Kocaeli": "İzmit", "Sakarya": "Adapazarı", "Hatay": "Antakya"}


def search(name):
    q = urllib.parse.urlencode({"name": name, "count": 30, "language": "tr", "countryCode": "TR", "format": "json"})
    for attempt in range(4):
        try:
            with urllib.request.urlopen(f"{API}?{q}", timeout=20) as r:
                return json.load(r).get("results", [])
        except Exception:
            time.sleep(1 + attempt)
    return []


def clean(name):
    for suffix in (" Province", " İlçesi", " District"):
        name = name.removesuffix(suffix)
    return name.split(" / ")[0].strip()


# GeoNames spellings that differ from the official district names
ALIASES = {"Şemdinni": "Şemdinli", "Istanbul": "İstanbul", "Hakkâri": "Hakkari", "Canakkale": "Çanakkale"}
# GeoNames areas that are not districts
SKIP = {"Doski Aşireti Bölgesi"}

admin1 = {}  # geonames admin1 code -> (name, geonameid)
for line in open(f"{GEO_DIR}/admin1CodesASCII.txt", encoding="utf8"):
    code, name, _ascii, gid = line.rstrip("\n").split("\t")
    if code.startswith("TR."):
        name = clean(name)
        admin1[code.split(".")[1]] = (ALIASES.get(name, name), int(gid))

districts = []  # (admin1 code, district name, geonameid)
admin2_code = {}  # district geonameid -> GeoNames admin2 code

# GeoNames Türkiye dump, for districts Open-Meteo doesn't know (mostly 2013+ metropolitan ones):
# district seat (PPLA2) by admin codes, else the district area itself
seats = {}
areas = {}
for line in open(f"{GEO_DIR}/TR.txt", encoding="utf8"):
    f = line.rstrip("\n").split("\t")
    gid, lat, lon, fcode, a1, a2 = int(f[0]), float(f[4]), float(f[5]), f[7], f[10], f[11]
    if fcode == "PPLA2":
        seats[(a1, a2)] = {"id": gid, "lat": lat, "lon": lon}
    elif fcode == "ADM2":
        areas[gid] = {"id": gid, "lat": lat, "lon": lon}
for line in open(f"{GEO_DIR}/admin2Codes.txt", encoding="utf8"):
    code, name, _ascii, gid = line.rstrip("\n").split("\t")
    if code.startswith("TR."):
        name = clean(name)
        if name in SKIP:
            continue
        districts.append((code.split(".")[1], ALIASES.get(name, name), int(gid)))
        admin2_code[int(gid)] = code.split(".")[2]


def resolve_province(item):
    code, (name, gid) = item
    lookup = CAPITALS.get(name, name)
    results = [r for r in search(lookup) if r.get("admin1_id") == gid]
    rank = {"PPLA": 0, "PPLC": 0, "ADM1": 2}
    results.sort(key=lambda r: (rank.get(r.get("feature_code"), 1), -(r.get("population") or 0)))
    if not results:
        return code, None
    r = results[0]
    return code, {"name": name, "id": r["id"], "lat": r["latitude"], "lon": r["longitude"], "admin1Id": gid}


def fold(text):
    """Same name ignoring Turkish letters, circumflexes, case and spaces."""
    table = str.maketrans("çğıöşüâîûÇĞİÖŞÜÂÎÛ", "cgiosuaiuCGIOSUAIU")
    return text.translate(table).casefold().replace(" ", "")


def has_turkish(text):
    return sum(ch in "çğıöşüÇĞİÖŞÜ" for ch in text)


def resolve_district(item):
    code, name, gid = item
    province_gid = admin1[code][1]
    # "Kilis Merkez" style central districts: search the town itself
    lookup = name.removesuffix(" Merkez") if name.endswith(" Merkez") else name
    found = search(lookup)
    rank = {"PPLA2": 0, "PPLA": 0, "PPLC": 0, "ADM2": 3}
    key = lambda r: (rank.get(r.get("feature_code"), 1), -(r.get("population") or 0))
    # Best: same GeoNames district id. Fallback (newer districts Open-Meteo doesn't link):
    # same province and same name
    exact = sorted((r for r in found if r.get("admin2_id") == gid or r.get("id") == gid), key=key)
    loose = sorted(
        (r for r in found if r.get("admin1_id") == province_gid and r.get("name") == lookup),
        key=key,
    )
    best = (exact or loose or [None])[0]
    if best is None:
        fallback = seats.get((code, admin2_code[gid])) or areas.get(gid)
        return code, name, fallback
    # Open-Meteo's Turkish admin2 name keeps ı/ş/ğ that some GeoNames names drop ("Beypazari")
    tr_name = next((clean(r["admin2"]) for r in exact if r.get("admin2_id") == gid and r.get("admin2")), None)
    if tr_name and fold(tr_name) == fold(name) and has_turkish(tr_name) >= has_turkish(name):
        name = tr_name
    elif tr_name and tr_name.casefold() != name.casefold():
        print("name differs, keeping GeoNames:", name, "vs", tr_name)
    return code, name, {"id": best["id"], "lat": best["latitude"], "lon": best["longitude"]}


with ThreadPoolExecutor(6) as pool:
    provinces = dict(pool.map(resolve_province, admin1.items()))
    resolved = list(pool.map(resolve_district, districts))

out = {"provinces": [], "unresolvedProvinces": [], "unresolvedDistricts": []}
for code, p in sorted(provinces.items()):
    name = admin1[code][0]
    if p is None:
        out["unresolvedProvinces"].append(name)
        continue
    p["districts"] = sorted(
        ({"name": n, **d} for c, n, d in resolved if c == code and d is not None),
        key=lambda d: d["name"],
    )
    out["provinces"].append(p)
out["unresolvedDistricts"] = [f"{admin1[c][0]}/{n}" for c, n, d in resolved if d is None]

json.dump(out, open(OUT, "w", encoding="utf8"), ensure_ascii=False, indent=1)
print("provinces", len(out["provinces"]), "unresolved provinces", out["unresolvedProvinces"])
print("districts", sum(len(p["districts"]) for p in out["provinces"]), "unresolved", len(out["unresolvedDistricts"]))
print(out["unresolvedDistricts"][:40])
