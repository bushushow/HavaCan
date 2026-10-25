"""Writes ProvinceCities.kt (81 provinces) and assets/districts_tr.json from places.json.
Usage: python emit_places.py <places.json> <TurkishProvinces.kt> <kotlin_out> <json_out>
"""
import json
import re
import sys

places_path, provinces_kt, kotlin_out, json_out = sys.argv[1:5]
places = json.load(open(places_path, encoding="utf8"))["provinces"]

# Plate codes from the app's own table, matched by name
plates = {name: int(plate) for plate, name in re.findall(r'Province\((\d+), "([^"]+)"', open(provinces_kt, encoding="utf8").read())}
by_name = {p["name"]: p for p in places}
missing = sorted(set(plates) - set(by_name))
extra = sorted(set(by_name) - set(plates))
if missing or extra:
    sys.exit(f"name mismatch: missing={missing} extra={extra}")

lines = []
for name, plate in sorted(plates.items(), key=lambda kv: kv[1]):
    p = by_name[name]
    lines.append(f'    province({p["id"]}, "{name}", {p["lat"]}, {p["lon"]}),')

kotlin = f"""package com.example.weather.data

import com.example.weather.model.City

// Generated from GeoNames (CC BY 4.0) province list and Open-Meteo coordinates.
// Do not edit by hand; see docs/data-sources.md.

/** All 81 provinces of Türkiye in plate order; coordinates are each province's capital. */
val provinceCities: List<City> = listOf(
{chr(10).join(lines)}
)

private fun province(id: Long, name: String, latitude: Double, longitude: Double) = City(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    countryCode = "TR",
    country = "Türkiye",
    admin1 = name,
)
"""
open(kotlin_out, "w", encoding="utf8").write(kotlin)

def fold(text):
    table = str.maketrans("çğıöşüâîûÇĞİÖŞÜÂÎÛ", "cgiosuaiuCGIOSUAIU")
    return text.translate(table).casefold().replace(" ", "")


def district_name(name, province):
    # Central districts named after the province ("Kirklareli", "Cankiri") take its proper spelling
    return province if fold(name) == fold(province) else name


districts = {
    str(plates[p["name"]]): [
        [district_name(d["name"], p["name"]), d["id"], round(d["lat"], 5), round(d["lon"], 5)] for d in p["districts"]
    ]
    for p in places
}
json.dump(districts, open(json_out, "w", encoding="utf8"), ensure_ascii=False, separators=(",", ":"))
print("provinces", len(lines), "districts", sum(len(v) for v in districts.values()))
