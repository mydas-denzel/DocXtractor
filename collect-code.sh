#!/bin/bash

# Output file
OUTPUT_FILE="collected_files.txt"

# Clear existing output
> "$OUTPUT_FILE"

# File types to search
FILE_TYPES=("*.java" "*.xml" "*.properties" "Dockerfile")

# Root directory (where script is run)
ROOT_DIR=$(pwd)

echo "Collecting files from: $ROOT_DIR"
echo "Output will be saved to: $OUTPUT_FILE"
echo ""

# Loop through each file type
for TYPE in "${FILE_TYPES[@]}"; do
    echo "Searching for $TYPE files..."

    # Find all matching files
    while IFS= read -r FILE; do
        REL_PATH=$(realpath --relative-to="$ROOT_DIR" "$FILE")

        {
            echo "========================================"
            echo "FILE: $REL_PATH"
            echo "========================================"
            cat "$FILE"
            echo -e "\n\n"
        } >> "$OUTPUT_FILE"

    done < <(find "$ROOT_DIR" -type f -name "$TYPE" 2>/dev/null)

done

echo "Done! All files collected into $OUTPUT_FILE"
