//Copyright (c) Microsoft Corporation All rights reserved.  
// 
//MIT License: 
// 
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
//documentation files (the  "Software"), to deal in the Software without restriction, including without limitation
//the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
//to permit persons to whom the Software is furnished to do so, subject to the following conditions: 
// 
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of
//the Software. 
// 
//THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
//TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
//THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
//CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.
package com.microsoft.band.sdk.sampleapp.accelerometer;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.sensors.BandAccelerometerEvent;
import com.microsoft.band.sensors.BandAccelerometerEventListener;
import com.microsoft.band.sensors.BandAmbientLightEventListener;
import com.microsoft.band.sensors.BandGyroscopeEvent;
import com.microsoft.band.sensors.BandGyroscopeEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;
import com.microsoft.band.sensors.BandAmbientLightEvent;
import com.microsoft.band.sensors.SampleRate;

import com.illposed.osc.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class BandAccelerometerAppActivity extends Activity {

	private BandClient client = null;
	private Button btnStart;
    private Button btnStop;
	private TextView txtStatus;

	private static OSCPortOut myOSCSender;

	//we need to do networking stuff in an AsyncTask to avoid NetworkOnMainThreadException
	private class sendOSCTask extends AsyncTask<OSCMessage, Integer, Long> {
		protected Long doInBackground(OSCMessage... msgs) {
			try {
				myOSCSender.send(msgs[0]);
			}
			catch (IOException e){
                e.printStackTrace();
			}


			return 0L;
		}
	};

	private BandAccelerometerEventListener mAccelerometerEventListener = new BandAccelerometerEventListener() {
        @Override
        public void onBandAccelerometerChanged(final BandAccelerometerEvent event) {
            if (event != null) {
            	appendToUI(String.format(" X = %.3f \n Y = %.3f\n Z = %.3f", event.getAccelerationX(),
            			event.getAccelerationY(), event.getAccelerationZ()));

				float x = event.getAccelerationX();
				float y = event.getAccelerationY();
				float z = event.getAccelerationZ();
				OSCMessage msg = new OSCMessage("/band/accel");
				msg.addArgument(x);
				msg.addArgument(y);
				msg.addArgument(z);
				new sendOSCTask().execute(msg);
            }
        }
    };

    private BandGyroscopeEventListener mGyroEventListener = new BandGyroscopeEventListener() {
        @Override
        public void onBandGyroscopeChanged(BandGyroscopeEvent bandGyroscopeEvent) {
            if (bandGyroscopeEvent != null) {
                float rx = bandGyroscopeEvent.getAngularVelocityX();
                float ry = bandGyroscopeEvent.getAngularVelocityY();
                float rz = bandGyroscopeEvent.getAngularVelocityZ();
                OSCMessage msg = new OSCMessage("/band/gyro");
                msg.addArgument(rx);
                msg.addArgument(ry);
                msg.addArgument(rz);
                new sendOSCTask().execute(msg);
            }
        }
    };

	private BandSkinTemperatureEventListener mSkinTemperatureEventListener = new BandSkinTemperatureEventListener() {
		@Override
		public void onBandSkinTemperatureChanged(BandSkinTemperatureEvent bandSkinTemperatureEvent) {
			if (bandSkinTemperatureEvent != null) {
				float skinTemp = bandSkinTemperatureEvent.getTemperature();
				OSCMessage msg = new OSCMessage("/band/skintemp");
				msg.addArgument(skinTemp);
				new sendOSCTask().execute(msg);

			}
		}
	};

    //note: needs to set up consent for this to work!!!
	private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
		@Override
		public void onBandHeartRateChanged(BandHeartRateEvent bandHeartRateEvent) {
			if (bandHeartRateEvent != null) {
				int hr = bandHeartRateEvent.getHeartRate();
				OSCMessage msg = new OSCMessage("/band/heartrate");
				msg.addArgument(hr);
				new sendOSCTask().execute(msg);
			}
		}
	};

    private BandAmbientLightEventListener mAmbientLightEventListener = new BandAmbientLightEventListener() {
        @Override
        public void onBandAmbientLightChanged(BandAmbientLightEvent bandAmbientLightEvent) {
            if (bandAmbientLightEvent != null) {
                int light = bandAmbientLightEvent.getBrightness();
                OSCMessage msg = new OSCMessage("/band/light");
                msg.addArgument(light);
                new sendOSCTask().execute(msg);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


		try {
			myOSCSender = new OSCPortOut(InetAddress.getByName("192.168.0.100"), 7000);
			OSCMessage msg = new OSCMessage("/hello/world");
			msg.addArgument("I_am_Band");
			new sendOSCTask().execute(msg);
		}
		catch (UnknownHostException e) {
			Log.e("ERR", "uknown");

		}
		catch (SocketException e) {
            e.printStackTrace();
		}


		txtStatus = (TextView) findViewById(R.id.txtStatus);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);

        final WeakReference<Activity> reference = new WeakReference<Activity>(this);

        btnStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				txtStatus.setText("");
				new SensorSubscriptionTask().execute();
			}
		});
        btnStop.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                txtStatus.setText("Streaming stopped");
                if (client != null) {
                    try {
                        client.getSensorManager().unregisterAllListeners();
                    }
                    catch (BandIOException e) {
                        appendToUI(e.getMessage());
                    }
                }

            }
        });
    }

    @Override
	protected void onResume() {
		super.onResume();
		txtStatus.setText("");
	}

    @Override
	protected void onPause() {
		super.onPause();
		if (client != null) {
			try {
				//client.getSensorManager().unregisterAccelerometerEventListener(mAccelerometerEventListener);
				//client.getSensorManager().unregisterSkinTemperatureEventListener(mSkinTemperatureEventListener);
                client.getSensorManager().unregisterAllListeners();
			} catch (BandIOException e) {
				appendToUI(e.getMessage());
			}
		}
	}

	private class SensorSubscriptionTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			try {
                if (getConnectedBandClient()) {
					appendToUI("Band is connected.\n");
					client.getSensorManager().registerAccelerometerEventListener(mAccelerometerEventListener, SampleRate.MS16);
					client.getSensorManager().registerSkinTemperatureEventListener(mSkinTemperatureEventListener);
                    client.getSensorManager().registerAmbientLightEventListener(mAmbientLightEventListener);
                    client.getSensorManager().registerGyroscopeEventListener(mGyroEventListener, SampleRate.MS32);

                    if (client.getSensorManager().getCurrentHeartRateConsent() == UserConsent.GRANTED) {
                        client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
                    }

				} else {
					appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
				}
			} catch (BandException e) {
				String exceptionMessage="";
				switch (e.getErrorType()) {
				case UNSUPPORTED_SDK_VERSION_ERROR:
					exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
					break;
				case SERVICE_ERROR:
					exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
					break;
				default:
					exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
					break;
				}
				appendToUI(exceptionMessage);

			} catch (Exception e) {
				appendToUI(e.getMessage());
			}
			return null;
		}
	}

    private class HeartRateConsentTask extends AsyncTask<WeakReference<Activity>, Void, Void> {
        @Override
        protected Void doInBackground(WeakReference<Activity>... params) {
            try {
                if (getConnectedBandClient()) {

                    if (params[0].get() != null) {
                        client.getSensorManager().requestHeartRateConsent(params[0].get(), new HeartRateConsentListener() {
                            @Override
                            public void userAccepted(boolean consentGiven) {
                            }
                        });
                    }
                } else {
                    appendToUI("Band isn't connected. Please make sure bluetooth is on and the band is in range.\n");
                }
            } catch (BandException e) {
                String exceptionMessage="";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.\n";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.\n";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage() + "\n";
                        break;
                }
                appendToUI(exceptionMessage);

            } catch (Exception e) {
                appendToUI(e.getMessage());
            }
            return null;
        }
    }


    @Override
    protected void onDestroy() {
        if (client != null) {
            try {
                client.disconnect().await();
            } catch (InterruptedException|BandException e) {
                // Do nothing as this is happening during destroy
                e.printStackTrace();
            }
        }
        super.onDestroy();
    }

	private void appendToUI(final String string) {
		this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	txtStatus.setText(string);
            }
        });
	}

	private boolean getConnectedBandClient() throws InterruptedException, BandException {
		if (client == null) {
			BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
			if (devices.length == 0) {
				appendToUI("Band isn't paired with your phone.\n");
				return false;
			}
			client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
		} else if (ConnectionState.CONNECTED == client.getConnectionState()) {
			return true;
		}

		appendToUI("Band is connecting...\n");
		return ConnectionState.CONNECTED == client.connect().await();
	}
}

