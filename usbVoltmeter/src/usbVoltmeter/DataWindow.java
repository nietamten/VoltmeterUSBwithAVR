package usbVoltmeter;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

import purejavahidapi.HidDeviceInfo;
import purejavahidapi.PureJavaHidApi;



public class DataWindow extends JFrame implements ActionListener, ListSelectionListener {

	static String convertStreamToString(java.io.InputStream is) {
	    @SuppressWarnings("resource")
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
	    return s.hasNext() ? s.next() : "";
	}
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	private static final long serialVersionUID = -6442339283148169538L;
	private JPanel contentPane;
	private XChartPanel<XYChart> chartPanel;
	private XYChart chart;

	private DefaultListModel<Device> listData = new DefaultListModel<Device>();
	private static JList<Device> list = null;
	
	public static void SetRegister(byte devNum, byte addr, byte val){
		byte[] dataa = new byte[2] ;
		dataa[0] = addr;
		dataa[1] = val;
		for (int i = 0; i < list.getModel().getSize(); i++) {
			if(devNum == -1 || list.getModel().getElementAt(i).GetNum() == devNum) {
				//list.getModel().getElementAt(i).SetFeature(dataa);
				list.getModel().getElementAt(i).dev.setFeatureReport(dataa, 2);
			}
		}
	}	

	private DataHub dataHub = null;

	/**
	 * Launch the application.
	 */
	public static void Show() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					DataWindow frame = new DataWindow();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private static XYChart createChart() {
		XYChart chart = new XYChartBuilder().xAxisTitle("time").yAxisTitle("value").build();
		chart.getStyler().setDatePattern("HH:mm:ss dd");
		chart.getStyler().setDecimalPattern("#0.000");
		chart.getStyler().setMarkerSize(2);
		return chart;
	}

	/**
	 * Create the frame.
	 * 
	 * @throws FileNotFoundException
	 */
	public DataWindow() {

	
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 907, 551);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		chart = createChart();
		chartPanel = new XChartPanel<XYChart>(chart);
		contentPane.add(chartPanel, BorderLayout.CENTER);

		JPanel pnlTop = new JPanel();
		contentPane.add(pnlTop, BorderLayout.NORTH);
		pnlTop.setLayout(new BoxLayout(pnlTop, BoxLayout.X_AXIS));

		JPanel panel_1 = new JPanel();
		pnlTop.add(panel_1);
		panel_1.setLayout(new BorderLayout(0, 0));


		list = new JList<Device>(listData);
		panel_1.add(list, BorderLayout.CENTER);
		list.addListSelectionListener(this);

		JPanel panel_2 = new JPanel();
		pnlTop.add(panel_2);
		panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.Y_AXIS));

		JPanel panel = new JPanel();
		panel_2.add(panel);
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
	
		
		
		JPanel panelB = new JPanel();
		contentPane.add(panelB, BorderLayout.PAGE_END);
		panelB.setLayout(new BoxLayout(panelB, BoxLayout.X_AXIS));

		JTextArea ta = new JTextArea(10,0);
		JScrollPane sp = new JScrollPane(ta); 		
		panelB.add(sp);
		
		try {
			FileInputStream inputStream = new FileInputStream("script.lua");
			ta.setText(convertStreamToString(inputStream));
			inputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		ta.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void changedUpdate(DocumentEvent arg0) {

			}

			@Override
			public void insertUpdate(DocumentEvent arg0) {
				try {
					FileOutputStream outputStream = new FileOutputStream("script.lua");
					outputStream.write(ta.getText().getBytes());
					outputStream.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void removeUpdate(DocumentEvent arg0) {
				try {
					FileOutputStream outputStream = new FileOutputStream("script.lua");
					outputStream.write(ta.getText().getBytes());
					outputStream.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		});
		
		JPanel panel_21 = new JPanel();
		panelB.add(panel_21);
		panel_21.setLayout(new BoxLayout(panel_21, BoxLayout.Y_AXIS));
		
		JButton btnNewButton_1 = new JButton("START");
		panel_21.add(btnNewButton_1);
		btnNewButton_1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dataHub.start();
			}
		});
		
		JButton btnNewButton_2 = new JButton("STOP");
		panel_21.add(btnNewButton_2);
		btnNewButton_2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				dataHub.stop();
			}
		});
		
		JPanel panel_22 = new JPanel();
		panelB.add(panel_22);
		panel_22.setLayout(new BoxLayout(panel_22, BoxLayout.X_AXIS));

		JList<Object> list = new JList<Object>();
		panel_22.add(list);		

			
		JPanel panel_3 = new JPanel();
		panel_2.add(panel_3);
		panel_3.setLayout(new BoxLayout(panel_3, BoxLayout.Y_AXIS));


		JPanel panel_5 = new JPanel();
		pnlTop.add(panel_5);
		panel_5.setLayout(new BoxLayout(panel_5, BoxLayout.Y_AXIS));

		dataHub = new DataHub(chart,chartPanel, list);
		scheduler.schedule(new DeviceListUpdater(), 0, TimeUnit.SECONDS);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

	}

	class DeviceListUpdater extends SwingWorker<List<HidDeviceInfo>, Object> {

		@Override
		public List<HidDeviceInfo> doInBackground() {
			List<HidDeviceInfo> devList = null;
			try {
				devList = PureJavaHidApi.enumerateDevices();

			} catch (Exception e) {
				e.printStackTrace();
			}
			return devList;
		}

		@Override
		protected void done() {
			List<HidDeviceInfo> devList = null;
			try {
				devList = get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			if (devList != null)
				try {

					// dopisywanie nowych
					for (HidDeviceInfo info : devList) {
						if (info.getVendorId() == 0x16c0 && info.getProductId() == 0x05df) {
							boolean present = false;
							for (int i = 0; i < list.getModel().getSize(); i++) {
								if (list.getModel().getElementAt(i).GetInfo().getPath().equals(info.getPath())) {
									present = true;
									break;
								}
							}
							if (!present) {
								byte num = 0;
								boolean found = true;
								while (found) {
									found = false;
									for (int i = 0; i < list.getModel().getSize(); i++) {
										if (num == list.getModel().getElementAt(i).GetNum()) {
											found = true;
											num++;
											break;
										}
									}
								}

								listData.addElement(new Device(info, dataHub, num));
							}
						}
					}
					// usuwanie brakujacych
					LinkedList<Integer> toDel = new LinkedList<Integer>();
					for (int i = 0; i < list.getModel().getSize(); i++) {
						boolean present = false;
						for (HidDeviceInfo info : devList) {
							if (info.getVendorId() == 0x16c0 && info.getProductId() == 0x05df) {
								if (list.getModel().getElementAt(i).GetInfo().getPath().equals(info.getPath())) {
									present = true;
									break;
								}
							}
						}
						if (!present) {
							toDel.add(i);
						}
					}
					ListIterator<Integer> li = toDel.listIterator(toDel.size());
					while (li.hasPrevious()) {
						listData.remove(li.previous());
					}

				} catch (Exception ignore) {
					ignore.printStackTrace();
				}
			scheduler.schedule(new DeviceListUpdater(), 3, TimeUnit.SECONDS);
		}
	}

	public int msecsOfFeatureReport(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
		buffer.put(bytes, 2, 1);
		buffer.put(bytes, 1, 1);
		buffer.flip();// need flip
		short tmp = buffer.getShort();
		int msecs = tmp >= 0 ? tmp : 0x10000 + tmp;
		return msecs;
	}

	@Override
	public void valueChanged(ListSelectionEvent arg0) {
		// TODO Auto-generated method stub
		
	}





}
