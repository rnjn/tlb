if [[ $# != 5 ]]; then
  echo "Usage: upload.sh <access_code> <source_file_path> <destination_file_name> <summary> <labels>"
  exit 1
fi

TLB_UPLOAD_PASSWORD=$1 TLB_UPLOAD_FILE_PATH=$2 TLB_UPLOAD_TARGET_FILE_NAME=$3 TLB_UPLOAD_SUMMARY="$4" TLB_UPLOAD_LABELS="$5" ant -Doffline=true upload-files

curl "http://