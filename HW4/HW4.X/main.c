#include "PIC32.h"       // constants, funcs for startup and UART
// Demonstrates spi by accessing external ram
// PIC is the master, ram is the slave
// Uses microchip 23K256 ram chip (see the data sheet for protocol details)
// SDO4 -> SI (pin F5 -> pin 5)
// SDI4 -> SO (pin F4 -> pin 2)
// SCK4 -> SCK (pin B14 -> pin 6)
// SS4 -> CS (pin B8 -> pin 1)
// Additional SRAM connections
// Vss (Pin 4) -> ground
// Vcc (Pin 8) -> 3.3 V
// Hold (pin 7) -> 3.3 V (we don't use the hold function)
// 
// Only uses the SRAM's sequential mode
//
#define CS LATBbits.LATB8       // chip select pin


// Initialize SPI1 Communication
void init_spi1(){
    TRISBbits.TRISB8 = 0;
    CS = 1;
    SS1Rbits.SS1R = 0b0100;
    SDI1Rbits.SDI1R = 0b0100;
    RPB13Rbits.RPB13R = 0b0011;
    SPI1CON = 0; // turn off the SPI1 module and reset it
    SPI1BUF; // clear the rx buffer by reading from it
    SPI1BRG = 0x1; // baud rate to 12 MHz [SPI4BRG = (48000000/(2*desired))-1]
    SPI1STATbits.SPIROV = 0; // clear the overflow bit
    SPI1CONbits.MODE32 = 0; // use 8 bit mode
    SPI1CONbits.MODE16 = 1;
    SPI1CONbits.CKE = 1;
    SPI1CONbits.MSTEN = 1; // master operation  
    SPI1CONbits.ON = 1; 
}

// send a byte via spi and return the response
unsigned char spi_io(unsigned char o) {
  SPI1BUF = o;
  while(!SPI1STATbits.SPIRBF) { // wait to receive the byte
    ;
  }
  return SPI1BUF;
}

//set Voltage function
//
void setVoltage(char channel, float voltage){
    int temp = voltage;
    CS = 0;
    spi_io((channel << 15) | 0x7000 | (temp << 4))
    CS = 1;
}

int main(void) {
    init_spi1();
    

    while(1) {
      ;
    }
    return 0;
}