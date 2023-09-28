cd "$(dirname "$0")"
mkdir build
cd build
cmake .. -DLIB_NAME:STRING="$1"
make

