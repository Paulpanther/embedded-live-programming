#pragma once

#include <iostream>
#include <dlfcn.h>
#include <vector>
#include "probe.h"
#include "debug_util.h"
#include "interface.tcc"

class UserCode {
    void* handle;
    void (*onClose)();

public:
    void (*setup)(Interface*);
    void (*loop)();
    std::vector<Probe>* probes;

    explicit UserCode(const char* path) {
        handle = dlopen(path, RTLD_LAZY);
        if (handle == nullptr) {
            std::cerr << dlerror() << std::endl;
            exit(1);
        }

        setup = (void (*)(Interface*)) dlsym(handle, "_setup");
        loop = (void (*)()) dlsym(handle, "_loop");
        onClose = (void (*)()) dlsym(handle, "onClose");
        probes = (std::vector<Probe>*) dlsym(handle, "probes");
    }

    void close() {
        onClose();
        dlclose(handle);
    }
};
