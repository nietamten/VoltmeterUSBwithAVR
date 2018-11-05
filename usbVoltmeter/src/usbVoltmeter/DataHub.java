package usbVoltmeter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JList;
import javax.swing.SwingUtilities;

import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

public class DataHub {

	static final public class LuaFunctions {
		private static class LuaPutOnGraph extends ThreeArgFunction {

			@Override
			public LuaValue call(LuaValue arg0, LuaValue arg1, LuaValue arg2) {
				addPoint(arg0.tobyte(), arg1.tolong(), arg2.todouble());
				return null;
			}

		}

		private static class LuaPutOnList extends TwoArgFunction {

			@Override
			public LuaValue call(LuaValue arg0, LuaValue arg1) {
				addValue(arg0.tojstring(), arg1.tojstring());
				return null;
			}

		}

		private static class LuaSetRegister extends ThreeArgFunction {

			@Override
			public LuaValue call(LuaValue arg0, LuaValue arg1, LuaValue arg2) {
				DataWindow.SetRegister(arg0.tobyte(), arg1.tobyte(), arg2.tobyte());
				return null;
			}

		}

		private static class LuaGraphLimit extends OneArgFunction {

			@Override
			public LuaValue call(LuaValue arg0) {
				graphMax = arg0.toint();
				return null;
			}

		}
	}

	private static int graphMax = 200;

	boolean stopped = true;

	private static XChartPanel<XYChart> chartPanel;
	private static XYChart chart = null;

	private static JList<Object> list;
	private static HashMap<String, String> lst = new HashMap<String, String>();

	static class SensorSeries {
		String sensorID;
		List<Date> x = new CopyOnWriteArrayList<Date>();
		List<Double> y = new CopyOnWriteArrayList<Double>();
	}

	public static Vector<SensorSeries> series;

	public DataHub(XYChart chart, XChartPanel<XYChart> chartPanel, JList<Object> list) {
		DataHub.chart = chart;
		DataHub.chartPanel = chartPanel;
		DataHub.list = list;
		series = new Vector<SensorSeries>();
	}

	private static void updateGUI(String n, String v) {
		lst.put(n, v);
		list.setListData(lst.entrySet().toArray());
	}

	private static void addValue(String n, String v) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				updateGUI(n, v);
			}
		});

	}

	private static void refreshGraph() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				chartPanel.updateUI();
			}
		});
  
	}

	private static void addPoint(byte devNum, long millis, double value) {
		while ((series.size() < devNum + 1))
			series.add(new SensorSeries());

		Instant instant = Instant.ofEpochMilli(millis);

		java.util.Date d = java.util.Date.from(instant);

		SensorSeries ss = series.elementAt(devNum);
		ss.x.add(d);
		ss.y.add((double) value);

		while (ss.x.size() > graphMax)
			ss.x.remove(0);
		while (ss.y.size() > graphMax)
			ss.y.remove(0);

		if (!chart.getSeriesMap().containsKey(Integer.toString(devNum)))
			chart.addSeries( Integer.toString(devNum) , ss.x, ss.y);

		chart.updateXYSeries(Integer.toString(devNum ), ss.x, ss.y, null);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				refreshGraph();
			}
		});
	}

	synchronized public void stop() {
		stopped = true;
	}

	private LuaValue _G;
	private LuaValue ONinitF;
	private LuaValue ONreadF;
	
	static String readFile(String path, Charset encoding) 
			  throws IOException 
			{
			  byte[] encoded = Files.readAllBytes(Paths.get(path));
			  return new String(encoded, encoding);
			}

	synchronized public void start() {
		lst.clear();
		_G = JsePlatform.standardGlobals();
		//_G.load(LuaValue.valueOf("./script.lua"));
		//_G.load(new FileReader(new File("s")));
	
		_G.get("dofile").call(LuaValue.valueOf("./script.lua"));

		{
			LuaValue LuaFunctions = CoerceJavaToLua.coerce(new LuaFunctions());
			_G.set("F", LuaFunctions);
			LuaTable t = new LuaTable();
			t.set("PutOnGraph", new LuaFunctions.LuaPutOnGraph());
			t.set("LuaPutOnList", new LuaFunctions.LuaPutOnList());
			t.set("LuaSetRegister", new LuaFunctions.LuaSetRegister());
			t.set("LuaGraphLimit", new LuaFunctions.LuaGraphLimit());
			t.set("__index", t);
			LuaFunctions.setmetatable(t);
		}

		ONinitF = _G.get("ONinit");
		ONreadF = _G.get("ONread");

		ONinitF.call();

		stopped = false;
	}

	synchronized public void write(byte probe[]) {
		if (!stopped) {

			long millis = timeOfProbe(probe);
			//Instant instant = Instant.ofEpochMilli(millis);

			double Raw = tempOfProbe(probe) ;// *((double)19/(double)512);

			byte devNum = probe[0];

			//System.out.println(instant + " d: " + devNum + " : " + Raw);

			ONreadF.call(LuaValue.valueOf(millis), LuaValue.valueOf(devNum), LuaValue.valueOf(Raw));
		}
	}

	public long timeOfProbe(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.put(bytes, 1, 8);
		buffer.flip();// need flip
		return buffer.getLong();
	}

	public short tempOfProbe(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.allocate(Short.BYTES);
		buffer.put(bytes, 10, 1);
		buffer.put(bytes, 9, 1);
		buffer.flip();// need flip
		short res = buffer.getShort();
		return res;
	}

	synchronized public void Reset(int maxCount, int skip, boolean autoscroll) {

	}
}

// https://stackoverflow.com/questions/11253420/calling-a-lua-function-from-luaj
// https://stackoverflow.com/questions/34665003/luaj-calling-a-java-method
