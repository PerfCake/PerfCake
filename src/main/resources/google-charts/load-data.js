function loadData() {
   jQuery.get('data.csv', function(csv) {
      var parsedData = $.csv.toArrays(csv, {separator: ';',
         onParseValue: function(value) {
            if (typeof(value) == 'string' && value.indexOf(':') > 0) {
               var timeTokens = value.split(':')
               return ((+timeTokens[0] * 3600000.0) + (+timeTokens[1] * 60000.0) + (+timeTokens[2] * 1000.0)) / 3600000.0
            } else {
               return $.csv.hooks.castToScalar(value)
            }
         }
      });
      drawChart(parsedData)
   }, 'text');
}