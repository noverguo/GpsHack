// IGpsService.aidl
package info.noverguo.gpshack;

// Declare any non-default types here with import statements

interface IGpsOffsetService {
    double getLatitudeOffset();
    double getLongitudeOffset();
    double getLatitude();
    double getLongitude();
}
