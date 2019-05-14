package com.igor_shaula.location_tracker.location;

import androidx.annotation.NonNull;

import com.igor_shaula.location_tracker.entity.LocationPoint;
import com.igor_shaula.location_tracker.utilities.GlobalKeys;
import com.igor_shaula.location_tracker.utilities.MyLog;

import java.util.List;

public final class LocationMath {

    // returns believable value of total passed distance \
    public static float getTotalDistance(@NonNull List <LocationPoint> locationPointList) {
        int capacity = locationPointList.size();
        MyLog.i("capacity = " + capacity);

        // preparing rewritable containers for the following loop \
        LocationPoint startPoint, endPoint;
        double startLat, startLong, endLat, endLong;
        float totalDistanceInMeters = 0;

        // getting all data and receiving numbers at every step \
        for (int i = 0 ; i < capacity ; i++) {
            // all works only if there are more than one point at all \
            if (locationPointList.iterator().hasNext()) {
                // this iterator is not from Collections framework - it's from Realm \
                MyLog.i("hasNext & i = " + i);

                // this is the simplest way to react on time negative difference \
                endPoint = locationPointList.get(i);
//            startPoint = locationPointList.get(i);
                endLat = endPoint.getLatitude();
//            startLat = startPoint.getLatitude();
                endLong = endPoint.getLongitude();
//            startLong = startPoint.getLongitude();
                startPoint = locationPointList.iterator().next();
//            endPoint = locationPointList.iterator().next();
                startLat = startPoint.getLatitude();
//            endLat = endPoint.getLatitude();
                startLong = startPoint.getLongitude();
//            endLong = endPoint.getLongitude();

                MyLog.i(i + " calculations: startPoint.millis = " + startPoint.getTime());
                MyLog.i(i + " calculations: endPoint.millis _ = " + endPoint.getTime());
                // somehow result was <0 otherwise - if end minus start \
                long deltaTime = (endPoint.getTime() - startPoint.getTime()) / 1000;
//                     long deltaTime = endPoint.getTime() - startPoint.getTime();
                MyLog.i(i + " calculations: seconds(end - start) = " + deltaTime);

                // we have to measure distance only between real points - not zeroes \
                if (startLat != 0.0 && startLong != 0.0 && endLat != 0.0 && endLong != 0.0) {

                    MyLog.i("startPoint speed = " + startPoint.getSpeed());

                    // before calculating distance checking if it could be real \
                    if (startPoint.getSpeed() > 1) { // meters per second

                        final float[] resultArray =
                                LocationConnector.getDistanceBetween(startLat , startLong , endLat , endLong);
                        MyLog.i(i + " calculations done: resultArray[0] = " + resultArray[0]);

                        // quick decision to cut off location noise and count only car movement \
                        if (resultArray[0] > GlobalKeys.MIN_DISTANCE_IN_METERS) {
                            /*
                             * i usually receive data with much higher values than it should be \
                             * so it's obvious that i have to make those strange values some lower \
                             * first simple attempt - to use measurement of speed to correct the result \
                             */
                            // excessive check '>0' because deltaTime was negative when 'end minus start' \
                            float predictedDistance = startPoint.getSpeed() * deltaTime;
//                     float predictedDistance = deltaTime > 0 ? startPoint.getSpeed() * deltaTime : 0;
                            MyLog.i(i + " calculations: predictedDistance = " + predictedDistance);

                            float minimumOfTwo = resultArray[0];
                            if (predictedDistance < minimumOfTwo)
//                     if (predictedDistance != 0 && predictedDistance < minimumOfTwo)
                                minimumOfTwo = predictedDistance;

                            totalDistanceInMeters += minimumOfTwo;
                        }
                    } // end of check-speed-condition \\
                } // end of check-four-non-zero-condition \\
            } // end of hasNext-condition \\
        } // end of for-loop \\
        MyLog.i("totalDistanceInMeters = " + totalDistanceInMeters);

        return totalDistanceInMeters;
    } // end of getTotalDistance-method \\
}