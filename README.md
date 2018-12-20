# VoltmeterUSBwithAVR
Attiny44 as USB differential voltmeter.

Design is somehow wrong ( see https://www.elektroda.pl/rtvforum/topic3442124.html ).
Voltages on AVR pins are higher than maximum from AVR documentation.
Using without USB isolation like ADUM4160 can destroy USB host.

To "repair" that design circuit can be changed to not use differential mode of ADC (changes in firmare can be ommited by using F.LuaSetRegister() ).

To assemble:
- flash one of hex files from firmware/bin directory depending of used crystal and change fuses to 0xDF both
- solder circuit from 'doc' directory, run usbVoltmeter/bin/usbVoltmeter/volt.jar then click "start" to get readings.

Device generates about 15 SPS (Samples Per Second). Reason is low VUSB speed.

Probe from device runs function from lua script (it is not visible), this function is ONread(timeOfProbe, deviceNumber, probeValue).

Lua funcion can put data on graph using PutOnGraph(x,y,value) or put on list using LuaPutOnList("name",value).

Clicking start button executes function ONinit().

There is some hack:

calling LuaSetRegister(devideNumber,register,value); (where deviceNum = -1 means all devices)

will write to AVR memory so it is possible to change for example ADC parameters.


-- this line sets ADC amplification 20x

--	F.LuaSetRegister(-1,0x27,0x11);

-- this line sets ADC amplification 1x

--	F.LuaSetRegister(-1,0x27,0x10);


Value can be find by reading table from page 154 of that pdf:
http://fab.cba.mit.edu/classes/863.09/people/ryan/week5/ATtiny44%20Data%20Sheet.pdf
Register number of MUX is in 16.13 chapter and in tabe at 211 page.

Hints:
- volt.jar need to be with script.lua in directory
- diodes act as voltage legulator and can be replaced with real one (see vusb page for details, take any diodes with usual voltage drop and it should work)

TODO (aka known bugs):
- java app not optimalized - every probe redraws whole plot and every point is drawed even tere is no space

Tested with success on Windows and Linux.

Building:

(1) make hardware

- (can be ommited - for updating purposes) copy "usbdrv" directory from https://github.com/obdev/v-usb to "firmware" direcotry of this repository; set crystal value in Makefile (look at F_CPU, default is for 12MHz) and type "make hex" in "firmware" directory
- connect attiny44 throu USBASP to upload firmware and change fuses by running (other than USBASP programmers need script changes) (a) upload.sh if building from source (b) bin/upload.sh where filename is one of hex files in this directory
- solder circuit and check it is detected by operating system (schema is in "doc" directory)

(2) make software (included binaries may not work on some machines)

- import prject from this repository to eclipse (manually or with egit from https://www.eclipse.org/egit/ File->Import->Git->Project from GIT->select 'dht' directory)
- (can be ommited - for updating purposes) copy to lib directory:
	+ jna-xxx.jar from https://github.com/java-native-access/jna
	+ purejavahidapi.jar from https://github.com/nyholku/purejavahidapi/tree/master/bin
	+ xchart-xxx.jar from https://knowm.org/open-source/xchart/xchart-change-log/
	+ luaj-jse-xxx.jar from https://sourceforge.net/projects/luaj/files/luaj-3.0/3.0.1/
then add to build path; in eclipse: right click on project->Properties->Java build path->Libraries->Classpath delete all existing then click AddJARs to select copied files
- build, run and test. Linux usually have no permission to device files, try run as root: 'chmod 777 /dev/hidraw*' AFTER connecting USB to be sure.
