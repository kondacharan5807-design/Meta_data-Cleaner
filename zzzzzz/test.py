from PIL import Image
import piexif

# Create a simple solid-color test image
img = Image.new('RGB', (800, 600), color='cyan')

# Define mock metadata (High-Risk PII and GPS)
exif_dict = {
    "0th": {
        piexif.ImageIFD.Make: b"MetaShield Test Camera",
        piexif.ImageIFD.Model: b"X-1000",
        piexif.ImageIFD.Artist: b"Charan Konda",
        piexif.ImageIFD.Software: b"Adobe Photoshop CS6"
    },
    "GPS": {
        # Mock GPS Coordinates (e.g., San Francisco)
        piexif.GPSIFD.GPSLatitudeRef: b'N',
        piexif.GPSIFD.GPSLatitude: ((37, 1), (46, 1), (30, 1)),
        piexif.GPSIFD.GPSLongitudeRef: b'W',
        piexif.GPSIFD.GPSLongitude: ((122, 1), (25, 1), (5, 1))
    }
}

# Dump the metadata into bytes
exif_bytes = piexif.dump(exif_dict)

# Save the image with the embedded EXIF data
output_filename = "sample_with_metadata.jpg"
img.save(output_filename, exif=exif_bytes)

print(f"Success: Created '{output_filename}' with embedded GPS, Author, and Device metadata.")