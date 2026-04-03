import argparse
import glob
import json
import os
import sys

CATEGORIES = [
    "FCMRInspectionMode",
    "AVViewerApplication",
    "CcaApplication",
    "AvaApplication",
    "FunctionalCardiacCTApplication",
]


def parse_percentage(pct_str):
    if not pct_str:
        return "0%"
    return str(pct_str).strip()


def main():
    parser = argparse.ArgumentParser(description="Parse and display workflow coverage summary")
    parser.add_argument("--logs", required=True, help="Path to folder containing data*.json files")
    args = parser.parse_args()

    pattern = os.path.join(args.logs, "data*.json")
    files = glob.glob(pattern)

    if not files:
        print("ERROR: No data*.json files found in the logs folder.")
        sys.exit(1)

    latest_file = max(files, key=os.path.getmtime)
    
    print("=" * 50)
    print("WORKFLOW COVERAGE SUMMARY")
    print("=" * 50)
    print("Reading: {}".format(os.path.basename(latest_file)))
    print("-" * 50)

    try:
        with open(latest_file, "r", encoding="utf-8") as f:
            data = json.load(f)
    except Exception as e:
        print("ERROR: Failed to read JSON file: {}".format(e))
        sys.exit(1)

    found_any = False
    for category in CATEGORIES:
        if category in data:
            try:
                inner = json.loads(data[category])
                percentage = parse_percentage(inner.get("Percentage :", "0%"))
                print("{} - {}".format(category, percentage))
                found_any = True
            except Exception as e:
                print("{} - ERROR parsing data".format(category))
        else:
            print("{} - NOT FOUND".format(category))

    print("=" * 50)

    if not found_any:
        print("\nWARNING: No valid coverage data found in the JSON file.")
        sys.exit(1)


if __name__ == "__main__":
    main()
