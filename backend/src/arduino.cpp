#include "arduino.h"

int fd;
Interface* interface;

Interface* getInterface(const std::string & port) {
    if (interface == nullptr) {
        initSerial(port);
    }
    return interface;
}

void initSerial(const std::string & port) {
    fd = open(port, O_RDWR | O_NOCTTY);
    if (fd < 0) {
        std::cout << "Error opening USB: " << strerror(errno) << std::endl;
        return;
    }

    struct termios tty;

    cfsetospeed (&tty, (speed_t)B9600);
    cfsetispeed (&tty, (speed_t)B9600);

    tty.c_cflag     &=  ~PARENB;            // Make 8n1
    tty.c_cflag     &=  ~CSTOPB;
    tty.c_cflag     &=  ~CSIZE;
    tty.c_cflag     |=  CS8;

    tty.c_cflag     &=  ~CRTSCTS;           // no flow control
    tty.c_cc[VMIN]   =  1;                  // read doesn't block
    tty.c_cc[VTIME]  =  0;
    tty.c_cflag     |=  CREAD | CLOCAL;     // turn on READ & ignore ctrl lines

    cfmakeraw(&tty);

    tcflush(fd, TCIFLUSH );
    if (tcsetattr (fd, TCSANOW, &tty ) != 0) {
        std::cout << "Error " << errno << " from tcsetattr" << std::endl;
    }

    std::this_thread::sleep_for(1s);

    if (fd == -1) return;

    interface = new Interface(fd);
    if (!(interface->status & STATUS_INITIALISED)) {
        return;
    }
}

void closeSerial() {
    delete interface;
}
