package com.jeizard.taxi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.PointF
import android.location.Address
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.jeizard.taxi.databinding.ActivityPathBinding
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouter
import com.yandex.mapkit.directions.driving.DrivingRouterType
import com.yandex.mapkit.directions.driving.DrivingSession
import com.yandex.mapkit.directions.driving.DrivingSession.DrivingRouteListener
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.logo.Alignment
import com.yandex.mapkit.logo.HorizontalAlignment
import com.yandex.mapkit.logo.VerticalAlignment
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider
import java.io.IOException
import java.util.Locale

class PathActivity : AppCompatActivity(), UserLocationObjectListener, CameraListener, DrivingRouteListener {
    private lateinit var binding: ActivityPathBinding

    private lateinit var checkLocationPermission: ActivityResultLauncher<Array<String>>

    private lateinit var userLocationLayer: UserLocationLayer

    private lateinit var iconStyle: IconStyle

    private var routeStartLocation = Point(0.0, 0.0)
    private var routeEndLocation = Point(0.0, 0.0)

    private var permissionLocation = false
    private var followUserLocation = false

    private var startMarker: PlacemarkMapObject? = null
    private var endMarker: PlacemarkMapObject? = null

    private var mapObject: MapObjectCollection? = null
    private var drivingRouter: DrivingRouter? = null
    private var drivingSession: DrivingSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context: Context = this

        initializeMapKitFactory(MAPKIT_API_KEY, this)

        binding = ActivityPathBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        iconStyle = IconStyle()
        iconStyle.anchor = PointF(0.5F, 1.0F)

        checkLocationPermission = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                onMapReady()
            }
        }

        checkPermission()
        userInterface()

        drivingRouter = DirectionsFactory.getInstance().createDrivingRouter(DrivingRouterType.ONLINE)
        mapObject = binding.mapView.map.mapObjects.addCollection()

        ArrayAdapter.createFromResource(
            this,
            R.array.route_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears.
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner.
            binding.routeStartLocation.adapter = adapter
        }

        binding.routeStartLocation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (parent != null) {
                    if(parent.getItemAtPosition(position) == "Map Point"){
                        binding.routeLayout.visibility = View.GONE
                        binding.setPointTextView.visibility = View.VISIBLE

                        binding.mapView.map.addInputListener(object : InputListener {
                            override fun onMapTap(map: Map, point: Point) {
                                selectAddress(this@PathActivity, point, binding.routeStartLocation){
                                    startMarker?.let {
                                        binding.mapView.map.mapObjects.remove(it)
                                    }
                                    startMarker = binding.mapView.map.mapObjects.addPlacemark(point)
                                    startMarker?.setIcon(ImageProvider.fromResource(context, R.drawable.ic_start_marker))

                                    startMarker?.setIconStyle(iconStyle)
                                }
                            }

                            override fun onMapLongTap(map: Map, point: Point) {
                                selectAddress(this@PathActivity, point, binding.routeStartLocation){
                                    startMarker?.let {
                                        binding.mapView.map.mapObjects.remove(it)
                                    }
                                    startMarker = binding.mapView.map.mapObjects.addPlacemark(point)
                                    startMarker?.setIcon(ImageProvider.fromResource(context, R.drawable.ic_start_marker))

                                    startMarker?.setIconStyle(iconStyle)
                                }
                            }
                        })
                    }
                }
            }
        }

        binding.routeEndLocation.setOnClickListener(View.OnClickListener {
            binding.routeLayout.visibility = View.GONE
            binding.setPointTextView.visibility = View.VISIBLE

            binding.mapView.map.addInputListener(object : InputListener {
                override fun onMapTap(map: Map, point: Point) {
                    selectAddress(this@PathActivity, point, binding.routeEndLocation){
                        endMarker?.let {
                            binding.mapView.map.mapObjects.remove(it)
                        }
                        endMarker = binding.mapView.map.mapObjects.addPlacemark(point)
                        endMarker?.setIcon(ImageProvider.fromResource(context, R.drawable.ic_marker))

                        endMarker?.setIconStyle(iconStyle)
                    }
                }

                override fun onMapLongTap(map: Map, point: Point) {
                    selectAddress(this@PathActivity, point, binding.routeEndLocation){
                        endMarker?.let {
                            binding.mapView.map.mapObjects.remove(it)
                        }
                        endMarker = binding.mapView.map.mapObjects.addPlacemark(point)
                        endMarker?.setIcon(ImageProvider.fromResource(context, R.drawable.ic_marker))

                        endMarker?.setIconStyle(iconStyle)
                    }
                }
            })
        })
    }

    private fun selectAddress(context: Context, point: Point, targetView: View, onClickAction: () -> Unit) {
        onClickAction()

        binding.mapView.map.move(
            CameraPosition(point, binding.mapView.map.cameraPosition.zoom, 0f, 0f),
            Animation(Animation.Type.SMOOTH, 1f),
            null
        )

        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            val addresses: MutableList<Address>? = geocoder.getFromLocation(point.latitude, point.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val addressLine = address.getAddressLine(0)
                binding.addressTextView.text = addressLine
                binding.addressLayout.visibility = View.VISIBLE
                binding.goHereButton.setOnClickListener(View.OnClickListener {
                    binding.addressLayout.visibility = View.GONE
                    binding.routeLayout.visibility = View.VISIBLE
                    binding.setPointTextView.visibility = View.GONE

                    targetView.setAddressAndPoint(addressLine, point)

                    if (routeEndLocation.latitude != 0.0 && routeEndLocation.longitude != 0.0) {
                        if(routeStartLocation.latitude != 0.0 && routeStartLocation.longitude != 0.0){
                            createRoute(routeStartLocation, routeEndLocation)
                        }
                        else{
                            createRoute(userLocationLayer.cameraPosition()!!.target, routeEndLocation)
                        }
                    }
                })
                Toast.makeText(context, address.getAddressLine(0), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Address not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Error getting address", Toast.LENGTH_SHORT).show()
        }
    }

    private fun View.setAddressAndPoint(addressLine: String, point: Point) {
        when (this) {
            is AppCompatButton -> {
                this.text = addressLine

                routeEndLocation = point
            }
            is Spinner -> {
                val adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, listOf(addressLine))
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                this.adapter = adapter

                routeStartLocation = point
            }
        }
    }


    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        binding.mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onMapReady()
        } else {
            checkLocationPermission.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun userInterface() {
        val mapLogoAlignment = Alignment(HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM)
        binding.mapView.map.logo.setAlignment(mapLogoAlignment)

        binding.userLocationFab.setOnClickListener {
            if (permissionLocation) {
                cameraUserPosition()

                followUserLocation = true
            } else {
                checkPermission()
            }
        }
    }

    private fun onMapReady() {
        val mapKit = MapKitFactory.getInstance()
        userLocationLayer = mapKit.createUserLocationLayer(binding.mapView.mapWindow)
        userLocationLayer.isVisible = true
        userLocationLayer.isHeadingEnabled = true
        userLocationLayer.setObjectListener(this)

        binding.mapView.map.addCameraListener(this)

        cameraUserPosition()

        permissionLocation = true
    }


    private fun cameraUserPosition() {
        if (userLocationLayer.cameraPosition() != null) {
            binding.mapView.map.move(
                CameraPosition(userLocationLayer.cameraPosition()!!.target, 16f, 0f, 0f), Animation(Animation.Type.SMOOTH, 1f), null
            )
        } else {
            binding.mapView.map.move(CameraPosition(Point(0.0, 0.0), 16f, 0f, 0f))
        }
    }

    override fun onCameraPositionChanged(
        map: Map, cPos: CameraPosition, cUpd: CameraUpdateReason, finish: Boolean
    ) {
        if (finish) {
            if (followUserLocation) {
                setAnchor()
            }
        } else {
            if (!followUserLocation) {
                noAnchor()
            }
        }
    }

    private fun setAnchor() {
        userLocationLayer.setAnchor(
            PointF(
                (binding.mapView.width * 0.5).toFloat(), (binding.mapView.height * 0.5).toFloat()
            ),
            PointF(
                (binding.mapView.width * 0.5).toFloat(), (binding.mapView.height * 0.83).toFloat()
            )
        )

        binding.userLocationFab.setImageResource(R.drawable.ic_my_location_black_24dp)

        followUserLocation = false
    }

    private fun noAnchor() {
        userLocationLayer.resetAnchor()

        binding.userLocationFab.setImageResource(R.drawable.ic_location_searching_black_24dp)
    }

    override fun onObjectAdded(userLocationView: UserLocationView) {
        setAnchor()

        userLocationView.pin.setIcon(ImageProvider.fromResource(this, R.drawable.user_arrow))
        userLocationView.arrow.setIcon(ImageProvider.fromResource(this, R.drawable.user_arrow))
    }

    override fun onObjectUpdated(p0: UserLocationView, p1: ObjectEvent) {}

    override fun onObjectRemoved(p0: UserLocationView) {}

    override fun onDrivingRoutes(p0: MutableList<DrivingRoute>) {
        mapObject!!.clear()
        mapObject!!.addPolyline(p0.first().geometry)
        Toast.makeText(this, "Distance: " + p0.first().metadata.weight.distance.text + ". Time: " + p0.first().metadata.weight.time.text, Toast.LENGTH_LONG).show()

        val distance: String = p0.first().metadata.weight.distance.text
        intent.putExtra("Distance", distance)
        val time: String = p0.first().metadata.weight.time.text
        intent.putExtra("Time", time)

        binding.routeInfTextView.visibility = View.VISIBLE
        binding.confirmButton.visibility = View.VISIBLE

        binding.routeInfTextView.text = "Distance: " + distance + ". Time: " + time
        binding.confirmButton.setOnClickListener(View.OnClickListener {
            setResult(RESULT_OK, intent);
            finish();
        })

//        for(route in p0){
//            mapObject!!.addPolyline(route.geometry)
//        }
    }

    override fun onDrivingRoutesError(p0: Error) {
        Toast.makeText(this, "Driving Routes Error", Toast.LENGTH_SHORT).show()
    }

    private fun createRoute(routeStartLocation: Point, routeEndLocation: Point) {
        val drivingOptions = DrivingOptions()
        val vehicleOptions = VehicleOptions()
        val requestPoints: ArrayList<RequestPoint> = ArrayList()
        RequestPoint()
        requestPoints.add(RequestPoint(routeStartLocation, RequestPointType.WAYPOINT, null, null))
        requestPoints.add(RequestPoint(routeEndLocation, RequestPointType.WAYPOINT, null, null))
        drivingSession = drivingRouter!!.requestRoutes(requestPoints, drivingOptions, vehicleOptions, this)

        val screenCenter = Point(
            (routeStartLocation.latitude + routeEndLocation.latitude) / 2,
            (routeStartLocation.longitude + routeEndLocation.longitude) / 2)
        binding.mapView.map.move(
            CameraPosition(screenCenter, binding.mapView.map.cameraPosition.zoom, 0f, 0f),
            Animation(Animation.Type.SMOOTH, 1f),
            null
        )

        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses: MutableList<Address>? = geocoder.getFromLocation(
                routeEndLocation.latitude,
                routeEndLocation.longitude,
                1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val addressLine = address.getAddressLine(0)
                val pointTo: String = addressLine
                intent.putExtra("Point To", pointTo)
            }
            val userAddresses: MutableList<Address>? = geocoder.getFromLocation(
                routeStartLocation.latitude,
                routeStartLocation.longitude,
                1
            )
            if (!userAddresses.isNullOrEmpty()) {
                val userAddress = userAddresses[0]
                val userAddressLine = userAddress.getAddressLine(0)
                val pointFrom: String = userAddressLine
                intent.putExtra("Point From", pointFrom)
            }
        }
        catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error getting address", Toast.LENGTH_SHORT).show()
        }
    }

    companion object{
        const val MAPKIT_API_KEY = "039b397d-6c73-425f-a38e-55389614cba8"

        private var initialized = false
        fun initializeMapKitFactory(apiKey: String, context: Context) {
            if (initialized) {
                return
            }

            MapKitFactory.setApiKey(apiKey)
            MapKitFactory.initialize(context)
            initialized = true
        }
    }
}