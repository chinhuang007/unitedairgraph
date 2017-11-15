def createDate(year, month, day, hour, min, sec) {
	c = Calendar.getInstance()
	c.clear()
	c.set(year, month - 1, day, hour, min, sec);
	return c.getTime()
}

def createTime(hour, min, sec) {
	c = Calendar.getInstance()
	c.clear()
	c.set(Calendar.HOUR, hour)
	c.set(Calendar.MINUTE, min)
	c.set(Calendar.SECOND, sec)
	return c.getTime()
}

def addTime(java.util.Date date, hour, min, sec) {
	c = Calendar.getInstance()
	c.setTime(date)
	c.add(Calendar.HOUR, hour)
	c.add(Calendar.MINUTE, min)
	c.add(Calendar.SECOND, sec)
	return c.getTime()
}

def getMCT(String dCountry, String aCountry, MCTdd, MCTdi, MCTid, MCTii) {
	MCTtime=0
	if (dCountry.equals("US")) {
		if (aCountry.equals("US"))
			MCTtime=MCTdd
		else
			MCTtime=MCTdi
	}
	else {
		if (aCountry.equals("US"))
			MCTtime=MCTid
		else
			MCTtime=MCTii
	}
	return MCTtime
}

def exceedMaxAllowableMileage(maxAllowableMileage, mileageList) {
	totalMileage = 0
	mileageList.each{ m ->
			totalMileage += m
	}
	return (totalMileage > maxAllowableMileage)
}

def isNotCircular(dAirport, aAirport, mAirport) {
	return (!mAirport.equals(dAirport)) && (!mAirport.equals(aAirport)) 
}

//def passRules(List countryList, List connectTimeList, List airportGroupList, List airportAliasList, List airlineList, List noflyList) {
//	dCountry = countryList[0]
//	aCountry = countryList[countryList.size-1]
//	
//	if (!passCabotage(countryList)) return false
//	if (exceedMaxConnectTime(dCountry, aCountry, connectTimeList)) return false
//	if (!passAliasMAC(airportGroupList)) return false
//	if (!passCircularRoutes(airportAliasList)) return false
//	if (!passIntMidpoints(countryList)) return false
//	if (!passKix(airlineList, noflyList)) return false
//	
//	return true
//}

def passCabotage(countryList) {
	for (i=0; i < countryList.size()-1; i++) {
		if (!countryList[i].equals("US"))
			if (countryList[i].equals(countryList[i+1]))
				return false
	}
	return true
}

def passNextDay(java.util.Date dTime, java.util.Date aTime) {
	a = Calendar.getInstance()
	a.setTime(aTime)
	d = Calendar.getInstance()
	d.setTime(dTime)

	if (d.after(a))
		return false
	else
		return true
}

def exceedMaxConnectTime(dCountry, aCountry, connectTimeList) {
	totalConnectTime = 0
	if (dCountry.equals(aCountry))
		maxCT = 6 * 60
	else
		maxCT = 24 * 60
	connectTimeList.each{ m ->
			totalConnectTime += m
	}
	return (totalConnectTime > maxCT)
}


def getConnectTime(java.util.Date aTime, java.util.Date dTime) {
	a = Calendar.getInstance()
	a.setTime(aTime)
	d = Calendar.getInstance()
	d.setTime(dTime)

	if (d.after(a))
		seconds = (d.getTimeInMillis() - a.getTimeInMillis()) / 1000
	else
		seconds = (d.getTimeInMillis() - a.getTimeInMillis() + 24 * 60 * 60 * 1000) / 1000
	return (int)(seconds/60)
}

def passAliasMAC(airportGroupList) {
	if (airportGroupList.size() > 0)
	    for (i=0; i < airportGroupList.size(); i++) {
	        if (airportGroupList.count(airportGroupList[i])>1)
	            return false
	    }
    return true
}

def passCircularRoutes(airportAliasList) {
	if (airportAliasList.size() > 0)
		for (i=0; i < airportAliasList.size(); i++) {
		    if (airportAliasList.count(airportAliasList[i])>1)
			    return false
		}
	return true
}

def passIntMidpoints(countryList) {
	startChecking=false
	outCountry=false
	
	for (i=0; i < countryList.size(); i++) {
		if (countryList[i].equals("US")) {
			startChecking = true
			if (outCountry)
				return false
		}
		else {
			if (startChecking)
				outCountry = true
		}	
	}
	return true
}

def passKix(airlineList, noflyList) {
	if (airlineList.size() > 0 && noflyList.size() > 0)
		for (i=0; i < airlineList.size(); i++) {
			if (noflyList.find{ it == airlineList[i]})
				return false
		}
	return true
}

def getMaxConnectTime(dCountry, aCountry) {
        if (dCountry.equals(aCountry))
                return 6 * 60
        else
                return 24 * 60
}

def convertMiles(miles) {
        return Integer.parseInt(miles)
}

