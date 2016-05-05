#include"PIC32.h"
#include<math.h>

#define CS LATBbits.LATB8 // chip select pin

static volatile float sineWave[200];   // sine waveform
static volatile float triangleWave[200];   // triangle waveform

// send a byte via spi and return the response
unsigned char spi_io(unsigned char o) {
  SPI1BUF = o;
  while(!SPI1STATbits.SPIRBF) { // wait to receive the byte
    ;
  }
  return SPI1BUF;
}

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



//set Voltage function
//
void setVoltage(char channel, float voltage){
    int temp = voltage;
    CS = 0;
    spi_io((channel << 15) | 0x7000 | (temp << 4));
    CS = 1;
}
//make the Sine and Triangle Waves
void makeWaves(){
    int i =0;
    for(i = 0; i < 200; i++) {
		triangleWave[i] = 255 * i / 200;
		sineWave[i] = 127.5 + 127.5 * sin(2 * 3.1415926 * 10 * (i % 100) / 1000);
	}  
}

int main(void) {
    
    
    init_spi1();
    makeWaves();
    
    while(1) {
        static int count = 0;
        if (_CP0_GET_COUNT() >= 24000) {
            _CP0_SET_COUNT(0);
            setVoltage(0, sineWave[count]);
            setVoltage(1, triangleWave[count]);
            count++;
            if (count > 199)
                count = 0;
        }
    }
    return 0;
}