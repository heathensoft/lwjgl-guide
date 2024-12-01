package io.github.heathensoft.guide.core;

import java.util.Comparator;
import java.util.List;

/**
 * * Most common desktop screen resolutions:
 * <a href="https://gs.statcounter.com/screen-resolution-stats/desktop/worldwide">statcounter</a>
 * Frederik Dahl 12/1/2024
 */
public record Resolution(int width, int height) implements Comparable<Resolution> {

    public static final Resolution R_1280x1024 = new Resolution(1280, 1024);     // 1.25
    public static final Resolution R_800x600 = new Resolution(800, 600);         // 1.33..
    public static final Resolution R_1024x768 = new Resolution(1024, 768);
    public static final Resolution R_1152x864 = new Resolution(1152, 864);
    public static final Resolution R_1280x960 = new Resolution(1280, 960);
    public static final Resolution R_1600x1024 = new Resolution(1600, 1024);     // 1.5625
    public static final Resolution R_1280x800 = new Resolution(1280, 800);       // 1.6
    public static final Resolution R_1920x1200 = new Resolution(1920, 1200);
    public static final Resolution R_1280x768 = new Resolution(1280, 768);       // 1.66..
    public static final Resolution R_1176_664 = new Resolution(1176, 664);       // 1.7711
    public static final Resolution R_1360x768 = new Resolution(1360, 768);
    public static final Resolution R_1280x720 = new Resolution(1280, 720);       // 1.77.. (HD)
    public static final Resolution R_1536x864 = new Resolution(1536, 864);
    public static final Resolution R_1600x900 = new Resolution(1600, 900);
    public static final Resolution R_1920x1080 = new Resolution(1920, 1080);
    public static final Resolution R_2560x1440 = new Resolution(2560, 1440);
    public static final Resolution R_3840x2160 = new Resolution(3840, 2160);
    public static final Resolution R_1366x768 = new Resolution(1366, 768);       // 1.7786
    public static final Resolution R_3440x1440 = new Resolution(3440, 1440);     // 2.38

    public int area() {
        return width * height;
    }

    public float aspect_ratio() {
        return (float) width / height;
    }

    @Override
    public int compareTo(Resolution o) {
        if (this.height == o.height) {
            if (this.width > o.width) return -1;
            else if (this.width < o.width) return 1;
            return 0;
        }
        if (this.height > o.height) return -1;
        else return 1;
    }

    @Override
    public String toString() {
        return "Resolution{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }

    /**
     * Sorts the options such as the first option in the array will be the resolution that
     * most closely fits the desired resolution. (What looks better)
     *
     * @param desired the desired resolution
     * @param options list
     */
    public static void sortByClosest(Resolution desired, List<Resolution> options) {
        if (options.size() > 1) {
            options.sort(new Comparator<>() {

                private final float[] comp_values = new float[2];

                private void calculate_values(Resolution r) {
                    // fit the resolution inside the desired resolution and calculate it's new size
                    float aspect_ratio = r.aspect_ratio();
                    float new_width = desired.width;
                    float new_height = new_width / aspect_ratio;
                    if (new_height > desired.height) {
                        new_height = desired.height;
                        new_width = new_height * aspect_ratio;
                    }
                    // the ratio of the scaling operation. How much the resolution changed (the smaller the ratio, the better)
                    float scale_variation = new_width > r.width ? new_width / r.width : r.width / new_width;
                    comp_values[0] = scale_variation;

                    // the ratio of wasted space after the scaling operation (the smaller, the better)
                    float desired_area = desired.width * desired.height;
                    float used_area = new_width * new_height;
                    float wasted_space = Math.abs(desired_area - used_area) / desired_area;
                    comp_values[1] = wasted_space;
                }

                @Override
                public int compare(Resolution r1, Resolution r2) {
                    if (null == r1) return null == r2 ? 0 : 1;
                    else if (null == r2) return -1;
                    calculate_values(r1);
                    float r1_scale_variation = comp_values[0];
                    float r1_wasted_space = comp_values[1];
                    calculate_values(r2);
                    float r2_scale_variation = comp_values[0];
                    float r2_wasted_space = comp_values[1];
                    int r1_comp = 0;
                    int r2_comp = 0;
                    if (r1_scale_variation < r2_scale_variation) r1_comp++;
                    else if (r1_scale_variation > r2_scale_variation) r2_comp++;
                    if (r1_wasted_space < r2_wasted_space) r1_comp++;
                    else if (r1_wasted_space > r2_wasted_space) r2_comp++;
                    return Integer.compare(r2_comp, r1_comp);
                }
            });
        }
    }
}
