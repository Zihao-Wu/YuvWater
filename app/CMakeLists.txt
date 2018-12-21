# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)


find_library( # Sets the name of the path variable.
              log-lib
              log )

# yuvOsd
add_library( # Sets the name of the library.
             YuvOsdUtils

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/YuvOsdUtils.c )


target_link_libraries(YuvOsdUtils
                       ${log-lib}

                       )