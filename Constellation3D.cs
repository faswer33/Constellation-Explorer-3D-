// Constellation3D.cs
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

class ConstellationData
{
    public string Abbr { get; set; }
    public string Mythology { get; set; }
    public string NotableObjects { get; set; }
    public List<object[]> Stars { get; set; } // [name, ra, dec, dist]
    public List<int[]> Connections { get; set; }
}

class StarMap3D
{
    public double Azimuth { get; set; } = 0;
    public double Elevation { get; set; } = 0;
    public double Scale { get; set; } = 1.0;
    public string Projection { get; set; } = "ortho";

    private static readonly Dictionary<string, ConstellationData> Constellations = new Dictionary<string, ConstellationData>();

    static StarMap3D()
    {
        var orion = new ConstellationData
        {
            Abbr = "Ori",
            Mythology = "Orion, a mighty hunter in Greek mythology, was placed among the stars by Zeus.",
            NotableObjects = "Orion Nebula (M42), Horsehead Nebula",
            Stars = new List<object[]> {
                new object[]{"Betelgeuse", 5.919, 7.407, 130.0},
                new object[]{"Bellatrix", 5.250, 6.350, 243.0},
                new object[]{"Mintaka", 5.650, -0.300, 200.0},
                new object[]{"Alnilam", 5.630, -1.200, 408.0},
                new object[]{"Alnitak", 5.620, -1.950, 200.0},
                new object[]{"Saiph", 5.230, -9.670, 198.0},
                new object[]{"Rigel", 5.240, -8.200, 264.0}
            },
            Connections = new List<int[]> {
                new int[]{0,1}, new int[]{0,2}, new int[]{0,3}, new int[]{0,4},
                new int[]{0,5}, new int[]{0,6}, new int[]{1,2}, new int[]{2,3},
                new int[]{3,4}, new int[]{4,5}, new int[]{5,6}, new int[]{2,4}
            }
        };
        Constellations["Orion"] = orion;

        var ursa = new ConstellationData
        {
            Abbr = "UMa",
            Mythology = "Ursa Major represents the Great Bear. In Greek mythology, Callisto was transformed into a bear by Hera.",
            NotableObjects = "M81, M82, Owl Nebula (M97)",
            Stars = new List<object[]> {
                new object[]{"Dubhe", 11.03, 61.75, 40.0},
                new object[]{"Merak", 11.02, 56.38, 24.0},
                new object[]{"Phecda", 11.85, 53.69, 28.0},
                new object[]{"Megrez", 12.15, 57.03, 22.0},
                new object[]{"Alioth", 12.90, 55.96, 24.0},
                new object[]{"Mizar", 13.23, 54.93, 23.0},
                new object[]{"Alkaid", 13.47, 49.31, 31.0}
            },
            Connections = new List<int[]> {
                new int[]{0,1}, new int[]{1,2}, new int[]{2,3},
                new int[]{3,4}, new int[]{4,5}, new int[]{5,6}
            }
        };
        Constellations["Ursa Major"] = ursa;

        var cass = new ConstellationData
        {
            Abbr = "Cas",
            Mythology = "Cassiopeia was the vain queen of Ethiopia who boasted about her beauty.",
            NotableObjects = "Cassiopeia A (supernova remnant)",
            Stars = new List<object[]> {
                new object[]{"Segin", 0.77, 60.72, 94.0},
                new object[]{"Ruchbah", 1.43, 60.66, 26.0},
                new object[]{"Schedar", 0.57, 56.54, 69.0},
                new object[]{"Navi", 1.18, 55.54, 22.0},
                new object[]{"Caph", 0.46, 59.15, 20.0}
            },
            Connections = new List<int[]> {
                new int[]{0,1}, new int[]{1,2}, new int[]{2,3},
                new int[]{3,4}, new int[]{0,2}, new int[]{2,4}
            }
        };
        Constellations["Cassiopeia"] = cass;

        var scorp = new ConstellationData
        {
            Abbr = "Sco",
            Mythology = "Scorpius represents the scorpion that killed Orion.",
            NotableObjects = "Antares (red supergiant), Ptolemy's Cluster",
            Stars = new List<object[]> {
                new object[]{"Antares", 16.30, -26.43, 170.0},
                new object[]{"Graffias", 16.00, -22.62, 280.0},
                new object[]{"Dschubba", 16.00, -22.62, 280.0},
                new object[]{"Wei", 16.20, -25.00, 300.0},
                new object[]{"Shaula", 17.44, -37.05, 200.0}
            },
            Connections = new List<int[]> {
                new int[]{0,1}, new int[]{1,2}, new int[]{2,3}, new int[]{3,4}
            }
        };
        Constellations["Scorpius"] = scorp;

        var lyra = new ConstellationData
        {
            Abbr = "Lyr",
            Mythology = "Lyra represents the lyre of Orpheus, the legendary musician.",
            NotableObjects = "Ring Nebula (M57), Vega",
            Stars = new List<object[]> {
                new object[]{"Vega", 18.62, 38.78, 7.7},
                new object[]{"Sheliak", 18.94, 33.36, 200.0},
                new object[]{"Sulafat", 18.50, 32.69, 200.0}
            },
            Connections = new List<int[]> {
                new int[]{0,1}, new int[]{0,2}, new int[]{1,2}
            }
        };
        Constellations["Lyra"] = lyra;
    }

    private double[] RaDecToCartesian(double raHours, double decDeg, double dist)
    {
        double raRad = raHours * 15 * Math.PI / 180;
        double decRad = decDeg * Math.PI / 180;
        double x = dist * Math.Cos(decRad) * Math.Cos(raRad);
        double y = dist * Math.Cos(decRad) * Math.Sin(raRad);
        double z = dist * Math.Sin(decRad);
        return new double[] { x, y, z };
    }

    private List<double[]> Rotate(List<double[]> points)
    {
        double az = Azimuth * Math.PI / 180;
        double el = Elevation * Math.PI / 180;
        var rotated = new List<double[]>();
        foreach (var p in points)
        {
            double x = p[0], y = p[1], z = p[2];
            double x1 = x * Math.Cos(az) + z * Math.Sin(az);
            double z1 = -x * Math.Sin(az) + z * Math.Cos(az);
            double y2 = y * Math.Cos(el) - z1 * Math.Sin(el);
            double z2 = y * Math.Sin(el) + z1 * Math.Cos(el);
            rotated.Add(new double[] { x1, y2, z2 });
        }
        return rotated;
    }

    private List<double[]> Project(List<double[]> points)
    {
        var screen = new List<double[]>();
        if (Projection == "persp")
        {
            double focal = 100;
            foreach (var p in points)
            {
                double x = p[0], y = p[1], z = p[2];
                if (z > -focal)
                {
                    double factor = focal / (z + focal);
                    screen.Add(new double[] { x * factor * Scale, y * factor * Scale });
                }
                else
                {
                    screen.Add(new double[] { double.NaN, double.NaN });
                }
            }
        }
        else
        {
            foreach (var p in points)
            {
                screen.Add(new double[] { p[0] * Scale, p[1] * Scale });
            }
        }
        return screen;
    }

    private void DrawLine(char[][] grid, int x0, int y0, int x1, int y1)
    {
        int dx = Math.Abs(x1 - x0);
        int dy = Math.Abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (true)
        {
            if (x0 >= 0 && x0 < grid[0].Length && y0 >= 0 && y0 < grid.Length)
            {
                if (grid[y0][x0] == ' ') grid[y0][x0] = '·';
            }
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }

    private string DrawASCII(List<double[]> screen, List<int[]> connections, int width, int height)
    {
        var valid = new List<(int idx, double x, double y)>();
        for (int i = 0; i < screen.Count; i++)
        {
            var p = screen[i];
            if (!double.IsNaN(p[0]))
            {
                valid.Add((i, p[0], p[1]));
            }
        }
        if (valid.Count == 0) return "No stars visible from this angle.";
        double minX = valid[0].x, maxX = valid[0].x;
        double minY = valid[0].y, maxY = valid[0].y;
        foreach (var v in valid)
        {
            if (v.x < minX) minX = v.x;
            if (v.x > maxX) maxX = v.x;
            if (v.y < minY) minY = v.y;
            if (v.y > maxY) maxY = v.y;
        }
        double rangeX = maxX - minX != 0 ? maxX - minX : 1;
        double rangeY = maxY - minY != 0 ? maxY - minY : 1;
        double scaleX = (width - 4) / rangeX;
        double scaleY = (height - 4) / rangeY;
        double scale = Math.Min(scaleX, scaleY);
        var grid = new char[height][];
        for (int i = 0; i < height; i++) grid[i] = new string(' ', width).ToCharArray();

        int[] ToGrid(double x, double y)
        {
            int cx = (int)((x - minX) * scale + 2);
            int cy = (int)((y - minY) * scale + 2);
            return new int[] { cx, cy };
        }

        var starPos = new Dictionary<int, int[]>();
        foreach (var v in valid)
        {
            var pos = ToGrid(v.x, v.y);
            int cx = pos[0], cy = pos[1];
            if (cx >= 0 && cx < width && cy >= 0 && cy < height)
            {
                grid[cy][cx] = '*';
                starPos[v.idx] = new int[] { cx, cy };
            }
        }
        foreach (var conn in connections)
        {
            if (starPos.ContainsKey(conn[0]) && starPos.ContainsKey(conn[1]))
            {
                var p1 = starPos[conn[0]];
                var p2 = starPos[conn[1]];
                DrawLine(grid, p1[0], p1[1], p2[0], p2[1]);
            }
        }
        var sb = new StringBuilder();
        foreach (var row in grid) sb.AppendLine(new string(row));
        return sb.ToString();
    }

    public string View(string name, double? az, double? el, double? scale, string proj)
    {
        if (az.HasValue) Azimuth = az.Value;
        if (el.HasValue) Elevation = el.Value;
        if (scale.HasValue) Scale = scale.Value;
        if (!string.IsNullOrEmpty(proj)) Projection = proj;

        if (!Constellations.TryGetValue(name, out var constData))
            return "Constellation '" + name + "' not found.";

        var points = new List<double[]>();
        foreach (var star in constData.Stars)
        {
            double ra = (double)star[1];
            double dec = (double)star[2];
            double dist = (double)star[3];
            points.Add(RaDecToCartesian(ra, dec, dist));
        }
        var rotated = Rotate(points);
        var screen = Project(rotated);
        var ascii = DrawASCII(screen, constData.Connections, 60, 20);
        var header = new StringBuilder();
        header.AppendLine($"\n🌟 Constellation: {name} ({constData.Abbr})");
        header.AppendLine($"Azimuth: {Azimuth:F1}°  Elevation: {Elevation:F1}°  Scale: {Scale:F1}");
        header.AppendLine($"Projection: {Projection}\n");
        return header.ToString() + ascii;
    }

    public static string ListConstellations()
    {
        var sb = new StringBuilder();
        sb.AppendLine("📋 Available Constellations:");
        foreach (var kv in Constellations)
            sb.AppendLine($"{kv.Key} ({kv.Value.Abbr})");
        return sb.ToString();
    }

    public static string Info(string name)
    {
        if (!Constellations.TryGetValue(name, out var data))
            return "Constellation '" + name + "' not found.";
        return $"\n✨ {name} ({data.Abbr})\nMythology: {data.Mythology}\nNotable Objects: {data.NotableObjects}\n";
    }

    static void Main(string[] args)
    {
        if (args.Length < 1)
        {
            Console.WriteLine("Usage: Constellation3D <command> [options]");
            return;
        }
        var cmd = args[0];
        var parsed = new Dictionary<string, string>();
        for (int i = 1; i < args.Length; i++)
        {
            if (args[i].StartsWith("--") && i + 1 < args.Length)
                parsed[args[i].Substring(2)] = args[++i];
            else if (args[i].StartsWith("--"))
                parsed[args[i].Substring(2)] = "";
        }
        var map = new StarMap3D();
        switch (cmd)
        {
            case "view":
                if (!parsed.TryGetValue("view", out var name)) name = null;
                if (string.IsNullOrEmpty(name) && args.Length > 1) name = args[1];
                if (string.IsNullOrEmpty(name)) { Console.WriteLine("view requires a constellation name"); return; }
                double? az = parsed.ContainsKey("az") ? double.Parse(parsed["az"]) : (double?)null;
                double? el = parsed.ContainsKey("el") ? double.Parse(parsed["el"]) : (double?)null;
                double? scale = parsed.ContainsKey("scale") ? double.Parse(parsed["scale"]) : (double?)null;
                string proj = parsed.ContainsKey("projection") ? parsed["projection"] : null;
                Console.Write(map.View(name, az, el, scale, proj));
                break;
            case "list":
                Console.Write(ListConstellations());
                break;
            case "info":
                if (!parsed.TryGetValue("info", out var infoName)) infoName = null;
                if (string.IsNullOrEmpty(infoName) && args.Length > 1) infoName = args[1];
                if (string.IsNullOrEmpty(infoName)) { Console.WriteLine("info requires a constellation name"); return; }
                Console.Write(Info(infoName));
                break;
            default:
                Console.WriteLine("Unknown command. Use view, list, info.");
                break;
        }
    }
}
