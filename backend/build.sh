rm -rf build
mkdir build
cd build
cmake -DCMAKE_INSTALL_PREFIX:PATH=. ..
cmake --build . --target install
mv lib/librunner.so ../runner.so
