<HTML>
<HEAD>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<link rel="stylesheet" type="text/css" href="/stylesheet.css">

<!--
<script src="http://yui.yahooapis.com/2.9.0/build/yahoo-dom-event/yahoo-dom-event.js"></script>
<script src="http://yui.yahooapis.com/2.9.0/build/dragdrop/dragdrop-min.js"></script>
<script src="http://yui.yahooapis.com/2.9.0/build/slider/slider-min.js"></script>
-->

<SCRIPT>

var studyUID = "1.2.840.113704.1.111.2512.1069112210.2";
var seriesUID = "1.2.840.113704.1.111.7860.1069113114.20";

var objectUIDs = new Array(

####REPLACEMEWITHLISTOFSOPINSTANCEUIDS####

);

var windowCenters = new Array(

####REPLACEMEWITHWINDOWCENTERS####

);

var windowWidths = new Array(

####REPLACEMEWITHWINDOWWIDTHS####

);

var numberOfImages = objectUIDs.length;

function preLoadImages() {
	var actualImages = new Array(numberOfImages);
	var i;
	for (i=0; i<numberOfImages; ++i) {
		actualImages[i] = new Image;
		actualImages[i].src = makeWadoURL(objectUIDs[i]);
	}
}

function dumpObjectUIDList() {
	document.write("<pre>\n");
	var i;
	for (i=0; i<numberOfImages; ++i) {
		document.write(objectUIDs[i] + "\n");
	}
	document.write("</pre>\n");
}


var currentImageIndex = 0;
var currentImageUrl = null;

var windowWidthDelta = x = 0;
var windowCenterDelta = y = 0;
		
function makeWadoURL(studyUID,seriesUID,objectUID) {
	return "?requestType=WADO&contentType=image/jpeg&studyUID=" + studyUID + "&seriesUID=" + seriesUID + "&objectUID=" + objectUID + "&columns=512" + "&imageQuality=100";
}

function makeWadoURLWithWindow(studyUID,seriesUID,objectUID,windowCenter,windowWidth) {
	return "?requestType=WADO&contentType=image/jpeg&studyUID=" + studyUID + "&seriesUID=" + seriesUID + "&objectUID=" + objectUID + "&windowCenter=" + windowCenter + "&windowWidth=" + windowWidth + "&columns=512" + "&imageQuality=100";
}

function loadCurrentImage() {
	//alert("loadCurrentImage()");
	var newWindowCenter = 0;
	var newWindowWidth = 0;
	if (windowWidths[currentImageIndex] > 0) {
		newWindowCenter = windowCenters[currentImageIndex]+windowCenterDelta;
		newWindowWidth = windowWidths[currentImageIndex]+windowWidthDelta;
		currentImageUrl=makeWadoURLWithWindow(studyUID,seriesUID,objectUIDs[currentImageIndex],newWindowCenter,newWindowWidth);
	}
	else {	// no valid window known
		currentImageUrl=makeWadoURL(studyUID,seriesUID,objectUIDs[currentImageIndex]);
	}
	//self.document.images[0].src=currentImageUrl;
	self.document.getElementById("target").src=currentImageUrl;
}

function replaceWithNextImage() {
	//alert("replaceWithNextImage()");
	if (++currentImageIndex >= numberOfImages) {
		currentImageIndex=0;
	}
	loadCurrentImage();
}

function loadFirstImage() {
	//alert("loadFirstImage()");
	//currentImageIndex=0;
	currentImageIndex=Math.floor(numberOfImages/2);
	loadCurrentImage();
	document.getElementById("target").onmousemove=ourMouseMove;
	document.getElementById("target").onmousedown=ourMouseDown;
	document.getElementById("target").onmouseup=ourMouseUp;

	document.getElementById("target").addEventListener("touchstart", touchHandler, true);
	document.getElementById("target").addEventListener("touchmove", touchHandler, true);
	document.getElementById("target").addEventListener("touchend", touchHandler, true);
}


//var slider = YAHOO.widget.Slider.getHorizSlider("sliderbg", "sliderthumb", 0, numberOfImages - 1);

//slider.subscribe('change', function (newImageIndex) {
//	if (newImageIndex < 0) { newImageIndex = 0; }
//	else if (newImageIndex >= numberOfImages) { newImageIndex = numberOfImages - 1; }
//	if (newImageIndex != currentImageIndex) {
//		currentImageIndex = newImageIndex;
//		loadCurrentImage();
//	}
//});

var mouseMode = 1;	// 1 == scroll, 2 == window

function setMouseModeToScroll() {
	//alert("setMouseModeToScroll()");
	mouseMode = 1;
}

function setMouseModeToWindow() {
	//alert("setMouseModeToWindow()");
	mouseMode = 2;
}


var mouseStartX = -1;
var mouseStartY = -1;
var mouseStartImageIndex = -1;
var mouseIsDown = 0;	// 0 = not down, 1 = down

var scrollDivisor = 5;

function handleDeltaOnMouseMove(x,y) {
	if (mouseMode == 1) {		// scroll
		y = Math.round(y/scrollDivisor);
		newImageIndex = mouseStartImageIndex + y;	// cannot comply with IHE BIR 4.16.4.2.2.5.5, since don't know direction of sort order within series and it may not be consistent :( (000668)
		if (newImageIndex < 0) { newImageIndex = 0; }
		else if (newImageIndex >= numberOfImages) { newImageIndex = numberOfImages - 1; }
		if (newImageIndex != currentImageIndex) {
			currentImageIndex = newImageIndex;
			loadCurrentImage();
		}
	}
}

function handleDeltaOnMouseUp(x,y) {
	if (mouseMode == 2) {		// window
		windowWidthDelta += x;	// IHE BIR 4.16.4.2.2.5.4 "horizontal movement of the mouse to the right will widen the window width (flatten the perceived contrast)"
		windowCenterDelta += y;	// IHE BIR 4.16.4.2.2.5.4 "vertical movement of the mouse upwards will lower the window center (increase the perceived brightness)"
		loadCurrentImage();
	}
}

function ourMouseDown(e) {
	//alert("ourMouseDown()");
	if (!e) { e = ((window.event) ? window.event : ""); }
	if (!e) {
		alert("ourMouseDown(): cannot get event for page position");
	}
	else {
		var posnX;
		var posnY;
		if ((posnX=e.pageX) == null || (posnY=e.pageY) == null) {	// Netscape and Safari
			posnX=event.clientX+document.body.scrollLeft;		// IE
			posnY=event.clientY+document.body.scrollTop;
		}
		mouseStartX = posnX;
		mouseStartY = posnY;
		mouseStartImageIndex = currentImageIndex;
		mouseIsDown = 1;
	}
	return false;		// stops image drag behavior in browser
}

var deltaX = 0;
var deltaY = 0;

function ourMouseUp(e) {
	if (deltaX != 0 || deltaY != 0) {
		//alert("ourMouseMove(): delta x="+deltaX+" y="+deltaY);
		if (mouseMode == 2) {		// window
			handleDeltaOnMouseUp(deltaX, deltaY);
		}
	}
	mouseIsDown = 0;
	mouseStartX = -1;
	mouseStartY = -1;
	mouseStartImageIndex = -1;
	return false;		// stops image drag behavior in browser
}

function ourMouseMove(e) {
	//alert("ourMouseMove()");
	if (!e) { e = ((window.event) ? window.event : ""); }
	if (!e) {
		alert("ourMouseMove(): cannot get event for page position");
	}
	else if (!mouseIsDown) {
		// do nothing
	}
	else if (mouseStartX == -1 || mouseStartY == -1) {
		alert("ourMouseMove(): illegal start x="+mouseStartX+" y="+mouseStartY);
	}
	else {
		var posnX;
		var posnY;
		if ((posnX=e.pageX) == null || (posnY=e.pageY) == null) {	// Netscape and Safari
			posnX=event.clientX+document.body.scrollLeft;		// IE
			posnY=event.clientY+document.body.scrollTop;
		}
		deltaX = posnX-mouseStartX;
		deltaY = posnY-mouseStartY;
		if (deltaX != 0 || deltaY != 0) {
			//alert("ourMouseMove(): delta x="+deltaX+" y="+deltaY);
			if (mouseMode == 1) {		// scroll
				handleDeltaOnMouseMove(deltaX, deltaY);
			}
		}
	}
	return false;		// stops image drag behavior in browser
}

// proxy touch events through to mouse events for mobile devices ...
// from "http://ross.posterous.com/2008/08/19/iphone-touch-events-in-javascript/"

function touchHandler(event) {
	var touches = event.changedTouches;
	var first = touches[0];
	var type = "";
	
	switch(event.type) {
		case "touchstart":	type = "mousedown"; break;
		case "touchmove":	type = "mousemove"; break;
		case "touchend":	type = "mouseup"; break;
		default: return;
	}
	var simulatedEvent = document.createEvent("MouseEvent");
	simulatedEvent.initMouseEvent(type, true, true, window, 1, first.screenX, first.screenY, first.clientX, first.clientY, false, false, false, false, 0/*left*/, null);
	
	first.target.dispatchEvent(simulatedEvent);
	event.preventDefault();
}

</SCRIPT>
</HEAD>


<BODY>

<SCRIPT>
//alert("starting");
//dumpObjectUIDList();
//document.write("<pre>"+currentImageUrl+"</pre>\n");
//preLoadImages()
</SCRIPT>

<!--
<table class="tablenoborder">
<tr>
<td>Slider Scrolls</td>
<td>
<div id="sliderbg">
    <div id="sliderthumb"><img src="http://yui.yahooapis.com/2.9.0/build/slider/assets/thumb-n.gif"></div>
</div>
</td>
</tr>
</table>
-->

<FORM>
<table class="tablenoborder">
<tr>
<td>Left Mouse or Finger Drag</td>
<td><input type="radio" name="mouseMode" value="Scrolls" checked onclick="setMouseModeToScroll()"> Scroll</td>
<td><input type="radio" name="mouseMode" value="Windows" onclick="setMouseModeToWindow()"> Window</td>
</tr>
</table>
</FORM>

<img id="target" src="" width="512">

<SCRIPT>loadFirstImage()</SCRIPT>

</BODY>
</HTML>
