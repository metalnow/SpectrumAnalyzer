/*
DIY-Spy grapher

Cheap and dirty hack to demonstrate the spectrum analyzer. Data comes in on the serial port (or USB faked variant thereof)
as 256-byte blocks of data corresponding to the 256 channels swept. The 0th channel's data is indicated by a '1' in the 
least significant bit; all others have this bit set to 0.

IMPORTANT: Change the value of 'comPort' below to reflect the port the device is attached to.

20090817 / tgipson
*/

import processing.serial.*;

// -------------------------------
String comPort = "COM18";        // CHANGE to correct serial port, if there is more than one. See the list that is printed when the program starts.

int nPoints = 256;        // Number of datapoints to display on the chart

int ChartX = 512;       // Dimensions of chart window
int ChartY = 512;
int heightPadding = 40; // space set aside for axis labeling

int scaler = 4; // amount to scale the data vertically

int rssi_offset = 72;  // CC2500's RSSI baseline offset in dBm.

//int LegendX = 700;      // Position of the chart legend on screen
//int LegendY = 60;

float freqMin = 2.400; // base frequency (ch. 0) in GHz
float freqStep = 0.000405; // channel spacing
float axisStep = .005; // axis display spacing

float freqMax = freqMin + (256*freqStep);  //2.50368;


// --------------------------------



Serial myPort;
PFont myFont;




int[] datapoints = new int[nPoints];
int[] maxes = new int[nPoints];
int[][] averages = new int[nPoints][2];

int chan=0;
int primed=0;
int rssi;


void setup()
{
  rectMode(CORNERS);
  ellipseMode(CENTER);
  size(ChartX, ChartY);
  print (datapoints.length);

  println(Serial.list());
  myPort = new Serial(this, comPort /*Serial.list()[0]*/, 9600, 'N', 8, 1);
  println(myPort.available());

  myFont = loadFont("ArialMT-48.vlw");
  textFont(myFont, 48);
  textSize(heightPadding/2);
}

void draw()
{
  background(0);
  graph();
}

void graph()
{
    pushStyle();

    stroke(255,255,255);
    fill(255,255,255);

  for (float i = freqMin; i < freqMax; i=i+axisStep)
  {
    pushMatrix();
    translate( map(i, freqMin, freqMax, 0, width), height-heightPadding); // (width*(i/freqMax))
    rotate(radians(90));
    text(i , 0, 0);
    popMatrix();    
  }

    popStyle();

  for (int i=0; i<datapoints.length; i++)
  {
    stroke(60,0,0);
    fill(60, 0, 0);
    rect(((width/256)*i) /**ChartX/datapoints.length*/, height-heightPadding, ((width/256)*(i+1))/**ChartX/datapoints.length*/, height - heightPadding - (height/256)*maxes[i]*scaler);


    if (averages[i][1] > 0)
    {
      stroke(0,0,255);
      fill(0, 0, 255);
      rect(((width/256)*i) /**ChartX/datapoints.length*/, height-heightPadding, ((width/256)*(i+1))/**ChartX/datapoints.length*/, height - heightPadding - (height/256)*(averages[i][0] / averages[i][1])*scaler);
    }

    stroke(60,255,60, 60);
    fill(60, 255, 60, 60);
    rect(((width/256)*i) /**ChartX/datapoints.length*/, height-heightPadding, ((width/256)*(i+1))/**ChartX/datapoints.length*/, height - heightPadding - (height/256)*datapoints[i]*scaler);
  }
}

void serialEvent(Serial myPort)
{
  rssi = myPort.readChar();
  if ((rssi & 0x01) == 0x01) // '1' in lowest bit indicates start of frame (0th channel data)
  {
    chan=0;
    primed = 1;
  }
  else
  {
    chan++;
  }


  // convert rssi byte to real output in dBm. After killing off LSB, output is in signed (dBm*2 + offset).
  rssi = rssi & 0xfe;
  rssi = ss(rssi);
  println("chan = " + chan + " rssi = " + rssi + " (" + binary(rssi, 8) + ")");
  rssi = rssi/2 - rssi_offset;

  if (primed != 0)
  {
      {
        datapoints[chan] = rssi;
        averages[chan][0] = averages[chan][0] + rssi;
        averages[chan][1] = averages[chan][1] + 1;
        if (rssi > maxes[chan])
        {
          maxes[chan] = rssi;
        }
      }
  }
}


int ss(int unsigned)
{
  if (unsigned < 128)
  {
    unsigned = (256 - unsigned);
  }
  return unsigned; // now signed...
}


