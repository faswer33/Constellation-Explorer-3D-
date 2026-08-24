// Constellation3D.java
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import java.lang.Math;

class ConstellationData {
    String abbr;
    String mythology;
    String notable_objects;
    List<Object[]> stars; // [name, ra, dec, dist]
    List<int[]> connections;

    // Gson will populate these
}

public class Constellation3D {
    private static final Map<String, ConstellationData> CONSTELLATIONS = new HashMap<>();

    static {
        // Manually build constellation data
        ConstellationData orion = new ConstellationData();
        orion.abbr = "Ori";
        orion.mythology = "Orion, a mighty hunter in Greek mythology, was placed among the stars by Zeus.";
        orion.notable_objects = "Orion Nebula (M42), Horsehead Nebula";
        orion.stars = Arrays.asList(
            new Object[]{"Betelgeuse", 5.919, 7.407, 130.0},
            new Object[]{"Bellatrix", 5.250, 6.350, 243.0},
            new Object[]{"Mintaka", 5.650, -0.300, 200.0},
            new Object[]{"Alnilam", 5.630, -1.200, 408.0},
            new Object[]{"Alnitak", 5.620, -1.950, 200.0},
            new Object[]{"Saiph", 5.230, -9.670, 198.0},
            new Object[]{"Rigel", 5.240, -8.200, 264.0}
        );
        orion.connections = Arrays.asList(
            new int[]{0,1}, new int[]{0,2}, new int[]{0,3}, new int[]{0,4},
            new int[]{0,5}, new int[]{0,6}, new int[]{1,2}, new int[]{2,3},
            new int[]{3,4}, new int[]{4,5}, new int[]{5,6}, new int[]{2,4}
        );
        CONSTELLATIONS.put("Orion", orion);

        ConstellationData ursa = new ConstellationData();
        ursa.abbr = "UMa";
        ursa.mythology = "Ursa Major represents the Great Bear. In Greek mythology, Callisto was transformed into a bear by Hera.";
        ursa.notable_objects = "M81, M82, Owl Nebula (M97)";
        ursa.stars = Arrays.asList(
            new Object[]{"Dubhe", 11.03, 61.75, 40.0},
            new Object[]{"Merak", 11.02, 56.38, 24.0},
            new Object[]{"Phecda", 11.85, 53.69, 28.0},
            new Object[]{"Megrez", 12.15, 57.03, 22.0},
            new Object[]{"Alioth", 12.90, 55.96, 24.0},
            new Object[]{"Mizar", 13.23, 54.93, 23.0},
            new Object[]{"Alkaid", 13.47, 49.31, 31.0}
        );
        ursa.connections = Arrays.asList(
            new int[]{0,1}, new int[]{1,2}, new int[]{2,3},
            new int[]{3,4}, new int[]{4,5}, new int[]{5,6}
        );
        CONSTELLATIONS.put("Ursa Major", ursa);

        ConstellationData cass = new ConstellationData();
        cass.abbr = "Cas";
        cass.mythology = "Cassiopeia was the vain queen of Ethiopia who boasted about her beauty.";
        cass.notable_objects = "Cassiopeia A (supernova remnant)";
        cass.stars = Arrays.asList(
            new Object[]{"Segin", 0.77, 60.72, 94.0},
            new Object[]{"Ruchbah", 1.43, 60.66, 26.0},
            new Object[]{"Schedar", 0.57, 56.54, 69.0},
            new Object[]{"Navi", 1.18, 55.54, 22.0},
            new Object[]{"Caph", 0.46, 59.15, 20.0}
        );
        cass.connections = Arrays.asList(
            new int[]{0,1}, new int[]{1,2}, new int[]{2,3},
            new int[]{3,4}, new int[]{0,2}, new int[]{2,4}
        );
        CONSTELLATIONS.put("Cassiopeia", cass);

        ConstellationData scorp = new ConstellationData();
        scorp.abbr = "Sco";
        scorp.mythology = "Scorpius represents the scorpion that killed Orion.";
        scorp.notable_objects = "Antares (red supergiant), Ptolemy's Cluster";
        scorp.stars = Arrays.asList(
            new Object[]{"Antares", 16.30, -26.43, 170.0},
            new Object[]{"Graffias", 16.00, -22.62, 280.0},
            new Object[]{"Dschubba", 16.00, -22.62, 280.0},
            new Object[]{"Wei", 16.20, -25.00, 300.0},
            new Object[]{"Shaula", 17.44, -37.05, 200.0}
        );
        scorp.connections = Arrays.asList(
            new int[]{0,1}, new int[]{1,2}, new int[]{2,3}, new int[]{3,4}
        );
        CONSTELLATIONS.put("Scorpius", scorp);

        ConstellationData lyra = new ConstellationData();
        lyra.abbr = "Lyr";
        lyra.mythology = "Lyra represents the lyre of Orpheus, the legendary musician.";
        lyra.notable_objects = "Ring Nebula (M57), Vega";
        lyra.stars = Arrays.asList(
            new Object[]{"Vega", 18.62, 38.78, 7.7},
            new Object[]{"Sheliak", 18.94, 33.36, 200.0},
            new Object[]{"Sulafat", 18.50, 32.69, 200.0}
        );
        lyra.connections = Arrays.asList(
            new int[]{0,1}, new int[]{0,2}, new int[]{1,2}
        );
        CONSTELLATIONS.put("Lyra", lyra);
    }

    static class StarMap3D {
        double azimuth = 0;
        double elevation = 0;
        double scale = 1.0;
        String projection = "ortho";

        double[] raDecToCartesian(double raHours, double decDeg, double dist) {
            double raRad = raHours * 15 * Math.PI / 180;
            double decRad = decDeg * Math.PI / 180;
            double x = dist * Math.cos(decRad) * Math.cos(raRad);
            double y = dist * Math.cos(decRad) * Math.sin(raRad);
            double z = dist * Math.sin(decRad);
            return new double[]{x, y, z};
        }

        List<double[]> rotate(List<double[]> points) {
            double az = azimuth * Math.PI / 180;
            double el = elevation * Math.PI / 180;
            List<double[]> rotated = new ArrayList<>();
            for (double[] p : points) {
                double x = p[0], y = p[1], z = p[2];
                // Y rotation
                double x1 = x * Math.cos(az) + z * Math.sin(az);
                double z1 = -x * Math.sin(az) + z * Math.cos(az);
                // X rotation
                double y2 = y * Math.cos(el) - z1 * Math.sin(el);
                double z2 = y * Math.sin(el) + z1 * Math.cos(el);
                rotated.add(new double[]{x1, y2, z2});
            }
            return rotated;
        }

        List<double[]> project(List<double[]> points) {
            List<double[]> screen = new ArrayList<>();
            if (projection.equals("persp")) {
                double focal = 100;
                for (double[] p : points) {
                    double x = p[0], y = p[1], z = p[2];
                    if (z > -focal) {
                        double factor = focal / (z + focal);
                        screen.add(new double[]{x * factor * scale, y * factor * scale});
                    } else {
                        screen.add(new double[]{Double.NaN, Double.NaN});
                    }
                }
            } else {
                for (double[] p : points) {
                    screen.add(new double[]{p[0] * scale, p[1] * scale});
                }
            }
            return screen;
        }

        void drawLine(char[][] grid, int x0, int y0, int x1, int y1) {
            int dx = Math.abs(x1 - x0);
            int dy = Math.abs(y1 - y0);
            int sx = x0 < x1 ? 1 : -1;
            int sy = y0 < y1 ? 1 : -1;
            int err = dx - dy;
            while (true) {
                if (x0 >= 0 && x0 < grid[0].length && y0 >= 0 && y0 < grid.length) {
                    if (grid[y0][x0] == ' ') {
                        grid[y0][x0] = '·';
                    }
                }
                if (x0 == x1 && y0 == y1) break;
                int e2 = 2 * err;
                if (e2 > -dy) { err -= dy; x0 += sx; }
                if (e2 < dx) { err += dx; y0 += sy; }
            }
        }

        String drawASCII(List<double[]> screen, List<int[]> connections, int width, int height) {
            List<int[]> valid = new ArrayList<>();
            for (int i = 0; i < screen.size(); i++) {
                double[] p = screen.get(i);
                if (!Double.isNaN(p[0])) {
                    valid.add(new int[]{i, (int)p[0], (int)p[1]}); // not ideal; we'll use doubles
                }
            }
            // Actually we need to keep doubles, so we'll store in a list of objects.
            List<Object[]> validPoints = new ArrayList<>();
            for (int i = 0; i < screen.size(); i++) {
                double[] p = screen.get(i);
                if (!Double.isNaN(p[0])) {
                    validPoints.add(new Object[]{i, p[0], p[1]});
                }
            }
            if (validPoints.isEmpty()) return "No stars visible from this angle.";
            double minX = (double)validPoints.get(0)[1], maxX = (double)validPoints.get(0)[1];
            double minY = (double)validPoints.get(0)[2], maxY = (double)validPoints.get(0)[2];
            for (Object[] v : validPoints) {
                double x = (double)v[1], y = (double)v[2];
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;
            }
            double rangeX = maxX - minX != 0 ? maxX - minX : 1;
            double rangeY = maxY - minY != 0 ? maxY - minY : 1;
            double scaleX = (width - 4) / rangeX;
            double scaleY = (height - 4) / rangeY;
            double scale = Math.min(scaleX, scaleY);
            char[][] grid = new char[height][width];
            for (int i = 0; i < height; i++) Arrays.fill(grid[i], ' ');
            java.util.function.BiFunction<Double, Double, int[]> toGrid = (x, y) -> {
                int cx = (int)((x - minX) * scale + 2);
                int cy = (int)((y - minY) * scale + 2);
                return new int[]{cx, cy};
            };
            Map<Integer, int[]> starPos = new HashMap<>();
            for (Object[] v : validPoints) {
                int idx = (int)v[0];
                double x = (double)v[1], y = (double)v[2];
                int[] pos = toGrid.apply(x, y);
                int cx = pos[0], cy = pos[1];
                if (cx >= 0 && cx < width && cy >= 0 && cy < height) {
                    grid[cy][cx] = '*';
                    starPos.put(idx, new int[]{cx, cy});
                }
            }
            for (int[] conn : connections) {
                if (starPos.containsKey(conn[0]) && starPos.containsKey(conn[1])) {
                    int[] p1 = starPos.get(conn[0]);
                    int[] p2 = starPos.get(conn[1]);
                    drawLine(grid, p1[0], p1[1], p2[0], p2[1]);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (char[] row : grid) {
                sb.append(new String(row)).append('\n');
            }
            return sb.toString();
        }

        String view(String name, Double az, Double el, Double scale, String proj) {
            if (az != null) this.azimuth = az;
            if (el != null) this.elevation = el;
            if (scale != null) this.scale = scale;
            if (proj != null) this.projection = proj;
            ConstellationData constData = CONSTELLATIONS.get(name);
            if (constData == null) return "Constellation '" + name + "' not found.";
            List<double[]> points = new ArrayList<>();
            for (Object[] star : constData.stars) {
                double ra = (double)star[1];
                double dec = (double)star[2];
                double dist = (double)star[3];
                points.add(raDecToCartesian(ra, dec, dist));
            }
            List<double[]> rotated = rotate(points);
            List<double[]> screen = project(rotated);
            String ascii = drawASCII(screen, constData.connections, 60, 20);
            StringBuilder header = new StringBuilder();
            header.append("\n🌟 Constellation: ").append(name).append(" (").append(constData.abbr).append(")\n");
            header.append(String.format("Azimuth: %.1f°  Elevation: %.1f°  Scale: %.1f\n", this.azimuth, this.elevation, this.scale));
            header.append("Projection: ").append(this.projection).append("\n\n");
            return header.toString() + ascii;
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: Constellation3D <command> [options]");
            return;
        }
        Map<String, String> params = new HashMap<>();
        for (int i = 1; i < args.length; i++) {
            if (args[i].startsWith("--") && i+1 < args.length) {
                params.put(args[i].substring(2), args[++i]);
            } else if (args[i].startsWith("--")) {
                params.put(args[i].substring(2), "");
            }
        }
        StarMap3D map = new StarMap3D();
        String cmd = args[0];
        switch (cmd) {
            case "view": {
                String name = params.get("view");
                if (name == null && args.length > 1) name = args[1];
                if (name == null) {
                    System.out.println("view requires a constellation name");
                    return;
                }
                Double az = params.containsKey("az") ? Double.parseDouble(params.get("az")) : null;
                Double el = params.containsKey("el") ? Double.parseDouble(params.get("el")) : null;
                Double scale = params.containsKey("scale") ? Double.parseDouble(params.get("scale")) : null;
                String proj = params.get("projection");
                System.out.print(map.view(name, az, el, scale, proj));
                break;
            }
            case "list":
                System.out.println("📋 Available Constellations:");
                for (String n : CONSTELLATIONS.keySet()) {
                    System.out.println(n + " (" + CONSTELLATIONS.get(n).abbr + ")");
                }
                break;
            case "info": {
                String name = params.get("info");
                if (name == null && args.length > 1) name = args[1];
                if (name == null) {
                    System.out.println("info requires a constellation name");
                    return;
                }
                ConstellationData data = CONSTELLATIONS.get(name);
                if (data == null) {
                    System.out.println("Constellation '" + name + "' not found.");
                    return;
                }
                System.out.println("\n✨ " + name + " (" + data.abbr + ")");
                System.out.println("Mythology: " + data.mythology);
                System.out.println("Notable Objects: " + data.notable_objects);
                break;
            }
            default:
                System.out.println("Unknown command. Use view, list, info.");
        }
    }
}
