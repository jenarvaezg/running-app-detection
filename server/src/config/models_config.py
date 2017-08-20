taps_features = [
    "n_accel",

    "accel_x_mean",
    "accel_x_median",
    "accel_x_var",
    "accel_x_skewness",
    "accel_x_kurtosis",
    "accel_x_diff",

    "accel_y_mean",
    "accel_y_median",
    "accel_y_skewness",
    "accel_y_kurtosis",
    "accel_y_var",
    "accel_y_diff",

    "accel_z_mean",
    "accel_z_var",
    "accel_z_median",
    "accel_z_skewness",
    "accel_z_kurtosis",
    "accel_z_diff",

    "n_gyro",

    "gyro_x_mean",
    "gyro_x_median",
    "gyro_x_var",
    "gyro_x_skewness",
    "gyro_x_kurtosis",
    "gyro_x_diff",

    "gyro_y_mean",
    "gyro_y_median",
    "gyro_y_var",
    "gyro_y_skewness",
    "gyro_y_kurtosis",
    "gyro_y_diff",

    "gyro_z_mean",
    "gyro_z_median",
    "gyro_z_var",
    "gyro_z_skewness",
    "gyro_z_kurtosis",
    "gyro_z_diff",

]


apps_features = [
    "words_tf_idf",
    "normalized_tf_idf",
    "time_diff_mean",
    "time_diff_var",
    "swipe_percentage",
]

# validation accuracy = 0.9723
noise_max_iterations = 640
noise_max_depth = 9

# validation accuracy = 0.9940
type_max_iterations = 640
type_max_depth = 8

# validation accuracy = 0.9931
swipe_max_iterations = 640
swipe_max_depth = 8

# validation accuracy = 0.9592
touch_max_iterations = 640
touch_max_depth = 8
