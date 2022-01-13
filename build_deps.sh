#!/bin/bash

set -u -e

WORK_DIR=""
OUTPUT_DIR=""

print_usage() {
  echo "Usage:"
  echo "  build_deps.sh --work_dir <WORK DIR> --output_dir <OUTPUT DIR>"
}

while (( "$#" )); do
  if [[ "$1" == "--work_dir" ]]; then
    WORK_DIR="$2"
  elif [[ "$1" == "--output_dir" ]]; then
    OUTPUT_DIR="$2"
  else
    echo "Error: unknown flag $1."
    print_usage
    exit 1
  fi
  shift 2
done

if [[ -z "${WORK_DIR}" ]]; then
  echo "Error: --work_dir is not specified."
  print_usage
  exit 1
fi
if [[ -z "${OUTPUT_DIR}" ]]; then
  echo "Error: --output_dir is not specified."
  print_usage
  exit 1
fi

# Clean up the workspace
rm -rf "${WORK_DIR}"
mkdir -p "${WORK_DIR}"

# At this point, we require OpenJDK 11.
readonly JNI_DIR="/usr/lib/jvm/java-11-openjdk-amd64/include"
readonly JNI_DIR_LINUX="/usr/lib/jvm/java-11-openjdk-amd64/include/linux"

# Data harmonization repo.
readonly DH_REPO="${DH_REPO:-https://github.com/GoogleCloudPlatform/healthcare-data-harmonization.git}"

if [[ ! -d "${JNI_DIR}" || ! -d "${JNI_DIR_LINUX}" ]]; then
  echo "Please make sure OpenJDK 11 is installed. On Debian/Ubuntu, run: sudo apt install openjdk-11-jdk"
  exit 1
fi


CURR_DIR="$(pwd)"
REPO_DIR="$(dirname "$0")"

echo "Cloning latest mapping engine code..."
git clone "${DH_REPO}" "${WORK_DIR}"

cp -r "${REPO_DIR}/deps/clib" "${WORK_DIR}/mapping_engine/clib"
cp -r "${REPO_DIR}/deps/wrapping" "${WORK_DIR}/mapping_engine/_wrapping"

echo "Building mapping engine..."

# Build the go libraries.
cd "${WORK_DIR}"
./build_all.sh

# Build the utility object.
cd mapping_engine
gcc -fPIC -Wl,--strip-all -c clib/mapping_util.c \
  -I"${JNI_DIR}" \
  -I"${JNI_DIR_LINUX}" \
  -o _wrapping/mapping_util.o

export C_INCLUDE_PATH="${JNI_DIR}:${JNI_DIR_LINUX}"

echo "Building wrapping..."
cd _wrapping
go mod init github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/wrapping
go mod edit -replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language=../../mapping_language
go mod edit -replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto=../proto
go mod edit -replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util=../util
go mod edit -replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine=../
go mod edit -replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/transpiler=../../mapping_language/transpiler
go mod edit -replace github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/transform=../transform

go get google.golang.org/genproto/googleapis/api/annotations@v0.0.0-20200526211855-cb27e3aa2013
go get google.golang.org/genproto/googleapis/rpc/status@v0.0.0-20200526211855-cb27e3aa2013
go get google.golang.org/grpc/binarylog/grpc_binarylog_v1@v1.27.1
go get google.golang.org/protobuf/testing/protocmp@v1.26.0
go get github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto
go get github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/transform
go get github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/transpiler@v0.0.0-20210315190620-fb0f05814962
go get google.golang.org/protobuf/encoding/prototext
go build -ldflags "-s -w" -o "${OUTPUT_DIR}/libwhistler.so" -buildmode=c-shared

# Clean up work directory
echo "Cleaning up temporary work directory..."
rm -rf "${WORK_DIR}"

unset C_INCLUDE_PATH

cd "${CURR_DIR}"
