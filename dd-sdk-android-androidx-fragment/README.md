# Datadog Integration to be able to use the TrackFragmentsAsViewsStrategy for AndroidX

## Getting Started 

To include the Datadog's Fragment tracking strategy in your project (based on Android's Jeptpack AppCompat library), 
simply add the following to your application's `build.gradle` file.


```
repositories {
    maven { url "https://dl.bintray.com/datadog/datadog-maven" }
}

dependencies {
    implementation "com.datadoghq:dd-sdk-android-androidx-fragment:<latest-version>"
    implementation "com.datadoghq:dd-sdk-android:<latest-version>"
}
```

**Note**: if you're using the Android Support Fragment library, use the [dd-sdk-support-fragment](dd-sdk-support-fragment) artifact instead.


### Initial Setup

Before you can use the SDK, you need to setup the library with your application
context and your API token. You can create a token from the Integrations > API
in Datadog. **Make sure you create a key of type `Client Token`.**

You can after initialize the Datadog SDK and set the **TrackFragmentsAaViewStrategy** 
as the current **viewTrackingStrategy** in the config file.

```kotlin
class SampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val config = DatadogConfig.Builder(BuildConfig.DD_CLIENT_TOKEN)
                     .setViewTrackingStrategy(TrackFragmentsAsViewsStrategy())   
                     .build()
        Datadog.initialize(this, config)
    }
}
```

## Contributing

Pull requests are welcome, but please open an issue first to discuss what you
would like to change. For more information, read the 
[Contributing Guide](../CONTRIBUTING.md).

## License

[Apache License, v2.0](../LICENSE)