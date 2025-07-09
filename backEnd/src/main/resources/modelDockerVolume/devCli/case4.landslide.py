from ogms_xfer import OGMS_Xfer as xfer
import datetime

config_file_path = "config.json"
xfer.initialize(config_file_path)

##################################################
start_time = datetime.datetime.now()

base_tif = xfer.URL.dataUrl('base.tif')
pga_tif = xfer.URL.dataUrl('pga.tif')
intensity_tif = xfer.URL.dataUrl('intensity.tif')

output_pga_tif = xfer.URL.outputUrl('outputPGApbtyTif.tif')
output_intensity_tif = xfer.URL.outputUrl('outputIntensitypbtyTif.tif')

xfer.modelStore.landslide_probability_model(base_tif, pga_tif, intensity_tif, output_pga_tif, output_intensity_tif)

end_time = datetime.datetime.now()
print(f"landslide_probability_model 计算完成，总时间: {end_time - start_time}")
print(' --------------------------------- ')