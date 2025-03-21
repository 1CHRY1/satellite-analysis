
// var dataset = ee.ImageCollection('Landsat8 OLI')
//                   .filterDate('2020-01-01', '2020-12-31')
//                   .filterSpatial(geojson)

// var result = Library.dataAggregation(dataset)

// Map.addLayer(result.visulizationInfo)
// Library.export(result.dataURL)


// NormalizedDifference example.
//
// Compute Normalized Difference Vegetation Index over MOD09GA product.
// NDVI = (NIR - RED) / (NIR + RED), whereXQ
// RED is sur_refl_b01, 620-670nm
// NIR is sur_refl_b02, 841-876nm



var dataset = ee.ImageCollection('Landsat8 OLI')
                  .filterDate('2020-01-01', '2020-12-31')
                  .filterSpatial(geojson) 

var ndviset = dataset.map(function(image){
    // Use the normalizedDifference(A, B) to compute (A - B) / (A + B)
    return image.calcNDVI(['band_b02', 'band_b01'])
})

Map.addSequenceLayer(ndviset, 'NDVI');
