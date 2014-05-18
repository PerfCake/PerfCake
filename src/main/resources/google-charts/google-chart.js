//google.load("visualization", "1", {packages : [ "corechart" ]});
google.setOnLoadCallback(loadData);

function drawChart(dataArray) {
	var data = google.visualization.arrayToDataTable(dataArray);
	var options = {
		enableInteractivity : true,
		title : 'Performance',
		height : 500,
		hAxis : {
			title : 'Time of test',
			minValue : 0.0,
			maxValue : 168.0,
			gridlines : {
				count : 15
			},
			minorGridlines : {
				count : 11
			},
			format : "##':00:00'"
		},
		vAxis : {
			title : 'Iterations per second',
			minValue : 0.0,
			// maxValue : 1200.0,
			gridlines : {
				count : 13
			},
			minorGridlines : {
				count : 1
			},
			format : "#.#"
		},
		linewidth : 1,
		pointSize : 0,
		series : {
			1 : {
				lineWidth : 0,
				pointSize : 1
			}
		}
	};
	var view = new google.visualization.DataView(data)
	view.setColumns([0, 4, 6])
	var chart = new google.visualization.LineChart(document.getElementById('chart_div'));
	chart.draw(view, options);
}