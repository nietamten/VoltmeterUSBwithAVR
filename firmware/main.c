#include <avr/io.h>
#include <avr/wdt.h>
#include <avr/interrupt.h>  /* for sei() */
#include <util/delay.h>     /* for _delay_ms() */
#include <avr/pgmspace.h>   /* required by usbdrv.h */
#include "usbdrv.h"
#include "usb_hid_sensors.h"
#include <string.h>  //memset
/* ------------------------------------------------------------------------- */
#define ASSERT_CONCAT_(a, b) a##b
#define ASSERT_CONCAT(a, b) ASSERT_CONCAT_(a, b)
#define ct_assert(e) enum { ASSERT_CONCAT(assert_line_, __LINE__) = 1/(!!(e)) }

PROGMEM const char usbHidReportDescriptor[] = {   

	//HID_USAGE_PAGE_SENSOR,
	//HID_USAGE_SENSOR_TYPE_OTHER_GENERIC,

		0x05,0x06, //Generic Device Controls Page 
		0x09,0x00, //undefined type

	HID_COLLECTION(Application),

		HID_USAGE_PAGE_SENSOR,

		HID_USAGE_SENSOR_DATA_GENERIC_DATAFIELD,
		HID_REPORT_SIZE(16),
		HID_REPORT_COUNT(1),
		HID_UNIT_EXPONENT(0), 
		HID_FEATURE(Data_Var_Abs),		
		
		HID_USAGE_SENSOR_TYPE_ELECTRICAL_VOLTAGE,
		HID_REPORT_SIZE(16),
		HID_REPORT_COUNT(1),
		HID_UNIT_EXPONENT(0x0F), // 1 digits past the decimal point
		HID_INPUT(Const_Var_Abs),
	
	HID_END_COLLECTION
	
}; 
ct_assert(sizeof(usbHidReportDescriptor)>29);
/* ------------------------------------------------------------------------- */
uchar usbFunctionWrite(uchar *data, uchar len)
{
	usbDisableAllRequests();
	volatile uchar *p = (void*)(unsigned int)data[0];
	*p = data[1];
	usbEnableAllRequests();
	return 1; //0xff = STALL
}
/* ------------------------------------------------------------------------- */
usbMsgLen_t usbFunctionSetup(uchar data[8])
{
	return USB_NO_MSG;
}
/* ------------------------------------------------------------------------- */
//for debugging with linux hidraw driver
void out_char(uchar c)
{
	while(!usbInterruptIsReady()){usbPoll();wdt_reset();}
	usbSetInterrupt((void *)&c, 1);
}
void numOut(unsigned long long num, uchar eol)
{
	uchar tmp[10];
	schar len;
	if(num==0)
	{
		out_char('0');
	}
	else
	{
		for(len=0;len<10;len++)
			tmp[len] = 0;
		
		len = 0;
		
		while(num!=0)
		{
			tmp[len++] = '0'+num%10;
			num /= 10;
		}
		
		for(len=9;len>=0;len--)
		{	
			if(tmp[len] != 0)
				out_char(tmp[len]);
		}
	}	
	if(eol)
	{
		out_char('\n');
	}
	else
	{
		out_char(' ');
	}	
}
void numOutH(unsigned long long num)
{
	uchar tmp[10];
	schar len;
	if(num==0)
	{
		out_char('0');
	}
	else
	{
		for(len=0;len<10;len++)
			tmp[len] = 0;
		
		len = 0;
		
		while(num!=0)
		{
			uint8_t r = num%16;
			if(r<=9)
				tmp[len++] = '0'+r;
			else
				tmp[len++] = 'A'+r-10;
			num /= 16;
		}
		
		for(len=9;len>=0;len--)
		{	
			if(tmp[len] != 0)
				out_char(tmp[len]);
		}
	}	
	out_char('\n');
}

/* ------------------------------------------------------------------------- */
/*ISR(ADC_vect)
{
}*/
/* ------------------------------------------------------------------------- */
int __attribute__((noreturn)) main(void)
{
	ADMUX |= (1 << MUX4);

	ADCSRA |= (1 << ADEN); // Enable ADC
	ADCSRA |= (1 << ADPS2) | (1 << ADPS1) | (1 << ADPS0); // prescaler to 128
	ADCSRB |= (1 << BIN);
	ADCSRA |= (1 << ADATE);  
	ADCSRA |= (1 << ADSC); // Start the conversion

    wdt_enable(WDTO_4S);
    usbInit();

    usbDeviceDisconnect();  // enforce re-enumeration, do this while interrupts are disabled! 
    uchar i = 0;
    while(--i){             // fake USB disconnect for > 250 ms 
        wdt_reset();
    }
    usbDeviceConnect();

    sei();
    for(;;){          
        wdt_reset();  
 		if(ADCSRA&(1<<ADIF) && usbInterruptIsReady())
		{
			ADCSRA|=(1<<ADIF);
			uint16_t ADCFull = ADCL; // get the ADCL 8 bits 
			ADCFull |= (ADCH<<8) ; 
			usbSetInterrupt((void *)&ADCFull, 2);
		}	
		usbPoll();
	}
}
/* ------------------------------------------------------------------------- */
		/* linux hidraw driver debugger
		if(ADCFull & 0x0200)
		{
			out_char('-');
			ADCFull &= ~0x0200;
			ADCFull = 0x01FF&(~ADCFull);
		}
		numOut(ADCFull,1);  	
		*/
