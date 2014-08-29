Custom ShowcaseView based on v5.1
====

This fork is for implementing that features I need in my Apps. Don't blame me if that does not work
for you. Please use the original project located at https://github.com/amlcurran/ShowcaseView.

My Changes
----

- Added Gingerbread compatibility (using [NineOldAndroids](https://github.com/JakeWharton/NineOldAndroids))
- Added multiple steps for a Showcase with an extended `Builder` and new `ShowcaseStep`s
- Removed `hideOnTouchOutside()` I moved that logic to the `ShowcaseStep` which can handle now that
- Removed unused resources and strings which comes with the Android SDK
- Using now circles for square views and rounded rectangles for other views
- Fixed lot of issues related with Gingerbread

Copyright and Licensing
----

Copyright Alex Curran ([+Alex](https://plus.google.com/110510888639261520925/posts)) © 2012. All
rights reserved.
*and me for my modifications* ;-)

This library is distributed under an Apache 2.0 License.
