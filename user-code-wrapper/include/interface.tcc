#ifndef CPP_SIMPLE_RPC_INTERFACE_TCC_
#define CPP_SIMPLE_RPC_INTERFACE_TCC_

#include <string>
#include <tuple>
#include <unordered_map>
#include <vector>
#include <fstream>
#include <iostream>

#include <unistd.h>
#include <ctime>
#include <iomanip>

#include "types.h"

#define STATUS_INITIALISED 0x01
#define STATUS_PROTOCOL_ERROR 0x02
#define STATUS_PROTOCOL_VERSION_ERROR 0x04
#define STATUS_PARAM_TYPE_WARNING 0x08
#define STATUS_RETURN_TYPE_WARNING 0x10

using std::get;
using std::string;
using std::tuple;
using std::unordered_map;
using std::vector;

extern unordered_map<char, int> _rpcTypeSize;

//! \defgroup interface

/*! \ingroup interface
 */
class Interface {
public:
    Interface(void) {}

    Interface(int);

    ~Interface(void);

    void open(int);

    void close(void);

    template<class T>
    void read(T *);

    template<class T>
    void read(T *, char);

    void read(string *);

    template<class T>
    void write(T *);

    template<class T>
    void write(T *, char);

    template<class R>
    R get(void);

    template<class T>
    void put(T const &);

    template<class R, class... Args>
    void call(R &, char const *, Args...);

    template<class R, class... Args>
    R call(char const *, Args...);

    template<class... Args>
    void call(char const *, Args...);

    uint8_t status = 0x00;  //!< Initialisation and error status.

//    template<class R>
//    void debug(const std::string &type, const R *msg, int size);

private:

    inline void _call(vector<string>) {}

    template<class T, class... Args>
    void _call(vector<string>, T const &, Args const &...);

    int _fd;
    uint8_t _endianness;
    uint8_t _sizeT;
    unordered_map<string, tuple<uint8_t, string, vector<string>, string>> _map;
    std::ofstream debugFile;
};


/*!
 * Read a value from a file descriptor.
 *
 * \param data Data.
 */
template<class T>
void Interface::read(T *data) {
    for (uint8_t i = 0; i < sizeof(T); i++) {
        ::read(_fd, &((uint8_t *) data)[i], 1);
    }
    //debug("read", data, sizeof(T));
}

/*!
 * Read a value from a file descriptor and perform type checking.
 *
 * When the type check fails, `status` flag `STATUS_RETURN_TYPE_WARNING` is
 * updated.
 *
 * \param data Data.
 * \param type Data type.
 */
template<class T>
void Interface::read(T *data, char type) {
    if (rpcTypeOf(*data) != type) {
        status |= STATUS_RETURN_TYPE_WARNING;
    }
    for (uint8_t i = 0; i < _rpcTypeSize[type]; i++) {
        ::read(_fd, &((uint8_t *) data)[i], 1);
    }
//    debug("read", data, _rpcTypeSize[type]);
}

/*!
 * Write a value to a file descriptor.
 *
 * \param data Data.
 */
template<class T>
void Interface::write(T *data) {
//    debug("write", data, sizeof(T));
    for (uint8_t i = 0; i < sizeof(T); i++) {
        ::write(_fd, &((uint8_t *) data)[i], 1);
    }
}

/*!
 * Write a value to a file descriptor and perform type checking.
 *
 * When the type check fails, `status` flag `STATUS_PARAM_TYPE_WARNING` is
 * updated.
 *
 * \param data Data.
 */
template<class T>
void Interface::write(T *data, char type) {
//    debug("write", data, _rpcTypeSize[type]);
    if (rpcTypeOf(*data) != type) {
        status |= STATUS_PARAM_TYPE_WARNING;
    }
    for (uint8_t i = 0; i < _rpcTypeSize[type]; i++) {
        ::write(_fd, &((uint8_t *) data)[i], 1);
    }
}

/*!
 * Read a value from a file descriptor (convenience function).
 *
 * \return Data.
 */
template<class R>
R Interface::get(void) {
    R data;
    read(&data);
    return data;
}

/*!
 * Write a value to a file descriptor (convenience function).
 *
 * \param data Data.
 */
template<class T>
void Interface::put(T const &data) {
    write(&data);
}

/*!
 * Write parameter values.
 *
 * \param sig Function parameters signature.
 * \param val Current parameter value.
 * \param args Remaining parameter values.
 */
template<class T, class... Args>
void Interface::_call(vector<string> sig, T const &val, Args const &... args) {
    write(&val, sig.front()[0]);
    sig.erase(sig.begin());
    _call(sig, args...);
}

/*!
 * Do an RPC call that does not return a value.
 *
 * The `status` flag `STATUS_PARAM_TYPE_WARNING` is cleared before the call is
 * executed.
 *
 * \param cmd RPC method name.
 * \param args RPC method parameter values.
 */
template<class... Args>
void Interface::call(char const *cmd, Args... args) {
    status &= ~STATUS_PARAM_TYPE_WARNING;
    write(&::get<0>(_map[cmd]));
    _call(::get<2>(_map[cmd]), args...);
}

/*!
 * Do an RPC call that returns a value.
 *
 * The `status` flag `STATUS_RETURN_TYPE_WARNING` is cleared before the call is
 * executed.
 *
 * \param data Return value of the RPC call.
 * \param cmd RPC method name.
 * \param args RPC method parameter values.
 */
template<class R, class... Args>
void Interface::call(R &data, char const *cmd, Args... args) {
    status &= ~STATUS_RETURN_TYPE_WARNING;
    call(cmd, args...);
    read(&data, ::get<1>(_map[cmd])[0]);

//    std::cout << cmd << " ";
//    ((std::cout << ", " << args), ...);
//    std::cout << ": " << data << std::endl;
}

/*!
 * Do an RPC call that returns a value.
 *
 * \param cmd RPC method name.
 * \param args RPC method parameter values.
 *
 * \return Return value of the RPC call.
 */
template<class R, class... Args>
R Interface::call(char const *cmd, Args... args) {
    R data;
    call(data, cmd, args...);
//    std::cout << cmd << " ";
//    ((std::cout << ", " << args), ...);
//    std::cout << ": " << data << std::endl;
    return data;
}

#endif
