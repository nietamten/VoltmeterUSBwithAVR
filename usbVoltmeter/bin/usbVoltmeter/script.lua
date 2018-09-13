function ONinit()
	F.LuaGraphLimit(150);
--	F.PutOnGraph(1,2,3);
--	F.LuaPutOnList("aaa","bbb");
-- this line sets ADC amplification 20x
--	F.LuaSetRegister(-1,0x27,0x11);
-- this line sets ADC amplification 1x
--	F.LuaSetRegister(-1,0x27,0x10);

end

vals = {};
valNum = 0;
function round(val, decimal)
  if (decimal) then
    return math.floor( (val * 10^decimal) + 0.5) / (10^decimal)
  else
    return math.floor(val+0.5)
  end
end

function ONread(time, device, value)
	value = value * 0.04;
max = 0;
		average = 0;
	if(valNum < 15) then
		vals[valNum] = value;
	else
		if(valNum >30) then
			valNum = 15
		end
		vals[valNum-15] = value;


		for i=0,14,1 do
			if max < vals[i] then
				max = vals[i];
			end
			average = average + vals[i];
		end
		F.LuaPutOnList("v",tostring(round(average/15,2)).."V");
F.LuaPutOnList("vm",tostring(round(max,2)).."V");

	end
	valNum = valNum + 1;

	F.PutOnGraph(device,time,value);
end