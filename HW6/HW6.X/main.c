#include <xc.h>
#include "ILI9163C.h"
#include "PIC32.h"
#include "i2c_master_noint.h"
// Demonstrate I2C by having the I2C1 talk to I2C5 on the same PIC32 (PIC32MX795F512H)
// Master will use SDA1 (D9) and SCL1 (D10).  Connect these through resistors to
// Vcc (3.3 V) (2.4k resistors recommended, but around that should be good enough)
// Slave will use SDA5 (F4) and SCL5 (F5)
// SDA5 -> SDA1
// SCL5 -> SCL1
// Two bytes will be written to the slave and then read back to the slave.
#define SLAVE_ADDR  0b1101011   
//=========================LCD Function======================

void SPI1_init() {
    SDI1Rbits.SDI1R = 0b0100; // B8 is SDI1
    RPA1Rbits.RPA1R = 0b0011; // A1 is SDO1
    TRISBbits.TRISB7 = 0; // SS is B7
    LATBbits.LATB7 = 1; // SS starts high

    // A0 / DAT pin
    ANSELBbits.ANSB15 = 0;
    TRISBbits.TRISB15 = 0;
    LATBbits.LATB15 = 0;

    SPI1CON = 0; // turn off the spi module and reset it
    SPI1BUF; // clear the rx buffer by reading from it
    SPI1BRG = 1; // baud rate to 12 MHz [SPI1BRG = (48000000/(2*desired))-1]
    SPI1STATbits.SPIROV = 0; // clear the overflow bit
    SPI1CONbits.CKE = 1; // data changes when clock goes from hi to lo (since CKP is 0)
    SPI1CONbits.MSTEN = 1; // master operation
    SPI1CONbits.ON = 1; // turn on spi1
}

unsigned char spi_io(unsigned char o) {
    SPI1BUF = o;
    while (!SPI1STATbits.SPIRBF) { // wait to receive the byte
        ;
    }
    return SPI1BUF;
}

void LCD_command(unsigned char com) {
    LATBbits.LATB15 = 0; // DAT
    LATBbits.LATB7 = 0; // CS
    spi_io(com);
    LATBbits.LATB7 = 1; // CS
}

void LCD_data(unsigned char dat) {
    LATBbits.LATB15 = 1; // DAT
    LATBbits.LATB7 = 0; // CS
    spi_io(dat);
    LATBbits.LATB7 = 1; // CS
}

void LCD_data16(unsigned short dat) {
    LATBbits.LATB15 = 1; // DAT
    LATBbits.LATB7 = 0; // CS
    spi_io(dat >> 8);
    spi_io(dat);
    LATBbits.LATB7 = 1; // CS
}

void LCD_init() {
    int time = 0;
    LCD_command(CMD_SWRESET); //software reset
    time = _CP0_GET_COUNT();
    while (_CP0_GET_COUNT() < time + 48000000 / 2 / 2) {
    } //delay(500);

    LCD_command(CMD_SLPOUT); //exit sleep
    time = _CP0_GET_COUNT();
    while (_CP0_GET_COUNT() < time + 48000000 / 2 / 200) {
    } //delay(5);

    LCD_command(CMD_PIXFMT); //Set Color Format 16bit
    LCD_data(0x05);
    time = _CP0_GET_COUNT();
    while (_CP0_GET_COUNT() < time + 48000000 / 2 / 200) {
    } //delay(5);

    LCD_command(CMD_GAMMASET); //default gamma curve 3
    LCD_data(0x04); //0x04
    time = _CP0_GET_COUNT();
    while (_CP0_GET_COUNT() < time + 48000000 / 2 / 1000) {
    } //delay(1);

    LCD_command(CMD_GAMRSEL); //Enable Gamma adj
    LCD_data(0x01);
    time = _CP0_GET_COUNT();
    while (_CP0_GET_COUNT() < time + 48000000 / 2 / 1000) {
    } //delay(1);

    LCD_command(CMD_NORML);

    LCD_command(CMD_DFUNCTR);
    LCD_data(0b11111111);
    LCD_data(0b00000110);

    int i = 0;
    LCD_command(CMD_PGAMMAC); //Positive Gamma Correction Setting
    for (i = 0; i < 15; i++) {
        LCD_data(pGammaSet[i]);
    }

    LCD_command(CMD_NGAMMAC); //Negative Gamma Correction Setting
    for (i = 0; i < 15; i++) {
        LCD_data(nGammaSet[i]);
    }

    LCD_command(CMD_FRMCTR1); //Frame Rate Control (In normal mode/Full colors)
    LCD_data(0x08); //0x0C//0x08
    LCD_data(0x02); //0x14//0x08
    time = _CP0_GET_COUNT();
    while (_CP0_GET_COUNT() < time + 48000000 / 2 / 1000) {
    } //delay(1);

    LCD_command(CMD_DINVCTR); //display inversion
    LCD_data(0x07);
    time = _CP0_GET_COUNT();
    while (_CP0_GET_COUNT() < time + 48000000 / 2 / 1000) {
    } //delay(1);

    LCD_command(CMD_PWCTR1); //Set VRH1[4:0] & VC[2:0] for VCI1 & GVDD
    LCD_data(0x0A); //4.30 - 0x0A
    LCD_data(0x02); //0x05
    time = _CP0_GET_COUNT();
    while (_CP0_GET_COUNT() < time + 48000000 / 2 / 1000) {
    } //delay(1);

    LCD_command(CMD_PWCTR2); //Set BT[2:0] for AVDD & VCL & VGH & VGL
    LCD_data(0x02);
    time = _CP0_GET_COUNT();
    while (_CP0_GET_COUNT() < time + 48000000 / 2 / 1000) {
    } //delay(1);

    LCD_command(CMD_VCOMCTR1); //Set VMH[6:0] & VML[6:0] for VOMH & VCOML
    LCD_data(0x50); //0x50
    LCD_data(99); //0x5b
    time = _CP0_GET_COUNT();
    while (_CP0_GET_COUNT() < time + 48000000 / 2 / 1000) {
    } //delay(1);

    LCD_command(CMD_VCOMOFFS);
    LCD_data(0); //0x40
    time = _CP0_GET_COUNT();
    while (_CP0_GET_COUNT() < time + 48000000 / 2 / 1000) {
    } //delay(1);

    LCD_command(CMD_CLMADRS); //Set Column Address
    LCD_data16(0x00);
    LCD_data16(_GRAMWIDTH);

    LCD_command(CMD_PGEADRS); //Set Page Address
    LCD_data16(0x00);
    LCD_data16(_GRAMHEIGH);

    LCD_command(CMD_VSCLLDEF);
    LCD_data16(0); // __OFFSET
    LCD_data16(_GRAMHEIGH); // _GRAMHEIGH - __OFFSET
    LCD_data16(0);

    LCD_command(CMD_MADCTL); // rotation
    LCD_data(0b00001000); // bit 3 0 for RGB, 1 for GBR, rotation: 0b00001000, 0b01101000, 0b11001000, 0b10101000

    LCD_command(CMD_DISPON); //display ON
    time = _CP0_GET_COUNT();
    while (_CP0_GET_COUNT() < time + 48000000 / 2 / 1000) {
    } //delay(1);

    LCD_command(CMD_RAMWR); //Memory Write
}

void LCD_drawPixel(unsigned short x, unsigned short y, unsigned short color) {
    // check boundary
    LCD_setAddr(x, y, x + 1, y + 1);
    LCD_data16(color);
}

void LCD_setAddr(unsigned short x0, unsigned short y0, unsigned short x1, unsigned short y1) {
    LCD_command(CMD_CLMADRS); // Column
    LCD_data16(x0);
    LCD_data16(x1);

    LCD_command(CMD_PGEADRS); // Page
    LCD_data16(y0);
    LCD_data16(y1);

    LCD_command(CMD_RAMWR); //Into RAM
}

void LCD_clearScreen(unsigned short color) {
    int i;
    LCD_setAddr(0, 0, _GRAMWIDTH, _GRAMHEIGH);
    for (i = 0; i < _GRAMSIZE; i++) {
        LCD_data16(color);
    }
}

void display_character(char c, unsigned short x, unsigned short y) {
    int temp = (int) c - 32;
    int col = 0;
    int row = 0;
    for (col = 0; col < 5; col++) {
        char c = 0x01;
        for (row = 0; row < 8; row++) {
            //            LCD_drawPixel(x+col,y+row,0xffff);
            if ((ASCII[temp][col] & c) >> row) {
                LCD_drawPixel(x + col, y + row, 0x07e0);
            } else {
                LCD_drawPixel(x + col, y + row, 0x0000);
            }
            c = c << 1;
        }
    }
}

//================END OF LCD FUNCTIONS ==============
void initIMU(void){
  i2c_master_start();
  i2c_master_send(SLAVE_ADDR << 1|0);      //IMU address
  i2c_master_send(0x10);                 //CTRL1_XL
  i2c_master_send(0b10000000);            //1.66 kHz, 2g, 400Hz
  i2c_master_stop();

  i2c_master_start();
  i2c_master_send(SLAVE_ADDR << 1|0);     //IMU address
  i2c_master_send(0x11);                //CTRL2_G
  i2c_master_send(0b1000001);            //1.66 kHz, 245 dps, gyroscope enabled
  i2c_master_stop();

  i2c_master_start();
  i2c_master_send(SLAVE_ADDR << 1|0);     //IMU address
  i2c_master_send(0x12);                //CTRL3_C
  i2c_master_send(0b00000100);           //IF_INC enabled
  i2c_master_stop();
}
//============== End of IMU Functions========

int main() {
    __builtin_disable_interrupts();

    // set the CP0 CONFIG register to indicate that kseg0 is cacheable (0x3)
    __builtin_mtc0(_CP0_CONFIG, _CP0_CONFIG_SELECT, 0xa4210583);

    // 0 data RAM access wait states
    BMXCONbits.BMXWSDRM = 0x0;

    // enable multi vector interrupts
    INTCONbits.MVEC = 0x1;

    // disable JTAG to get pins back
    DDPCONbits.JTAGEN = 0;

    i2c_master_setup();
    initIMU();

    __builtin_enable_interrupts();
    SPI1_init();
    LCD_init();

    LCD_clearScreen(0x0000);
    
    
    char message[200];
//        sprintf(message, "Master Read: 0x%b", master_read0);
        sprintf(message, "Read:");
        int i = 0;
        int c = 0;
        while (message[i]) {
            display_character(message[i], 10 + (c % 19)*6, 10 + 10 * (c / 19));
            i++;
            c++;
        }
    
    
    // some initialization function to set the right speed setting
    unsigned char master_write0 = 0x0F;       // register to read from 
    unsigned char master_write1 = 0x91;       // 
    unsigned char master_read0  = 0x00;       // first received byte


    while(1) {
        i2c_master_start();                     // Begin the start sequence
        i2c_master_send((SLAVE_ADDR << 1) | 0);       // send the slave address, left shifted by 1, 
                                              // which clears bit 0, indicating a write
        i2c_master_send(0x0F);         // send a byte to the slave       
        i2c_master_restart();                   // send a RESTART so we can begin reading 
        i2c_master_send((SLAVE_ADDR << 1) | 1); // send slave address, left shifted by 1,
                                              // and then a 1 in lsb, indicating read
        master_read0 = i2c_master_recv();       // receive a byte from the bus
        i2c_master_ack(1);                      // send ACK (0): master wants another byte!
        i2c_master_stop();                      // send STOP:  end transmission, give up bus
        
        sprintf(message, "Read: 0x%x", master_read0);
//        sprintf(message, "Read: 0x");
        int i = 0;
        int c = 0;
        while (message[i]) {
            display_character(message[i], 10 + (c % 19)*6, 10 + 10 * (c / 19));
            i++;
            c++;
        }
//        num = num + 1;
//        if(num > 1000){
//            num = 0;
//        }
        
          
    }
    return 0;
}