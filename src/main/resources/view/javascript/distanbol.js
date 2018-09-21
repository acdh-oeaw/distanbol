  const markerSource = new ol.source.Vector();

  var markerStyle = new ol.style.Style({
    image: new ol.style.Icon(/** @type {olx.style.IconOptions} */ ({
      anchor: [0.5, 46],
      anchorXUnits: 'fraction',
      anchorYUnits: 'pixels',
      opacity: 0.75,
      src: 'https://openlayers.org/en/v4.6.4/examples/data/icon.png'
    }))
  });

  let map = new ol.Map({
    target: 'map',
    layers: [
      new ol.layer.Tile({
        source: new ol.source.OSM(),
      }),
      new ol.layer.Vector({
        source: markerSource,
        style: markerStyle,
      }),
    ],
    view: new ol.View({
      center: ol.proj.fromLonLat([6.661594, 43.433237]),
      zoom: 1.81,
    })
  });


  function addMarker(lon, lat) {
//    console.log(lat);
//    console.log(lon);

    var iconFeatures = [];

    var iconFeature = new ol.Feature({
      geometry: new ol.geom.Point(ol.proj.transform([lon, lat], 'EPSG:4326',
        'EPSG:3857'))
    });

    markerSource.addFeature(iconFeature);
  }

var coordinates = document.body.getElementsByTagName("coordinate");

for(var i = 0, len = coordinates.length;i<len;i++){
    addMarker(parseFloat(coordinates[i].getElementsByTagName("long")[0].textContent),parseFloat(coordinates[i].getElementsByTagName("lat")[0].textContent));
}

//
//  addMarker(0,0);
//  addMarker(10,10);