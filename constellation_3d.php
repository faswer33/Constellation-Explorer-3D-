# constellation_3d.php
#!/usr/bin/env php
<?php

$CONSTELLATIONS = [
    "Orion" => [
        "abbr" => "Ori",
        "mythology" => "Orion, a mighty hunter in Greek mythology, was placed among the stars by Zeus.",
        "notable_objects" => "Orion Nebula (M42), Horsehead Nebula",
        "stars" => [
            ["Betelgeuse", 5.919, 7.407, 130],
            ["Bellatrix", 5.250, 6.350, 243],
            ["Mintaka", 5.650, -0.300, 200],
            ["Alnilam", 5.630, -1.200, 408],
            ["Alnitak", 5.620, -1.950, 200],
            ["Saiph", 5.230, -9.670, 198],
            ["Rigel", 5.240, -8.200, 264],
        ],
        "connections" => [[0,1],[0,2],[0,3],[0,4],[0,5],[0,6],[1,2],[2,3],[3,4],[4,5],[5,6],[2,4]]
    ],
    "Ursa Major" => [
        "abbr" => "UMa",
        "mythology" => "Ursa Major represents the Great Bear. In Greek mythology, Callisto was transformed into a bear by Hera.",
        "notable_objects" => "M81, M82, Owl Nebula (M97)",
        "stars" => [
            ["Dubhe", 11.03, 61.75, 40],
            ["Merak", 11.02, 56.38, 24],
            ["Phecda", 11.85, 53.69, 28],
            ["Megrez", 12.15, 57.03, 22],
            ["Alioth", 12.90, 55.96, 24],
            ["Mizar", 13.23, 54.93, 23],
            ["Alkaid", 13.47, 49.31, 31],
        ],
        "connections" => [[0,1],[1,2],[2,3],[3,4],[4,5],[5,6]]
    ],
    "Cassiopeia" => [
        "abbr" => "Cas",
        "mythology" => "Cassiopeia was the vain queen of Ethiopia who boasted about her beauty.",
        "notable_objects" => "Cassiopeia A (supernova remnant)",
        "stars" => [
            ["Segin", 0.77, 60.72, 94],
            ["Ruchbah", 1.43, 60.66, 26],
            ["Schedar", 0.57, 56.54, 69],
            ["Navi", 1.18, 55.54, 22],
            ["Caph", 0.46, 59.15, 20],
        ],
        "connections" => [[0,1],[1,2],[2,3],[3,4],[0,2],[2,4]]
    ],
    "Scorpius" => [
        "abbr" => "Sco",
        "mythology" => "Scorpius represents the scorpion that killed Orion.",
        "notable_objects" => "Antares (red supergiant), Ptolemy's Cluster",
        "stars" => [
            ["Antares", 16.30, -26.43, 170],
            ["Graffias", 16.00, -22.62, 280],
            ["Dschubba", 16.00, -22.62, 280],
            ["Wei", 16.20, -25.00, 300],
            ["Shaula", 17.44, -37.05, 200],
        ],
        "connections" => [[0,1],[1,2],[2,3],[3,4]]
    ],
    "Lyra" => [
        "abbr" => "Lyr",
        "mythology" => "Lyra represents the lyre of Orpheus, the legendary musician.",
        "notable_objects" => "Ring Nebula (M57), Vega",
        "stars" => [
            ["Vega", 18.62, 38.78, 7.7],
            ["Sheliak", 18.94, 33.36, 200],
            ["Sulafat", 18.50, 32.69, 200],
        ],
        "connections" => [[0,1],[0,2],[1,2]]
    ]
];

class StarMap3D {
    public $azimuth;
    public $elevation;
    public $scale;
    public $projection;

    public function __construct() {
        $this->azimuth = 0;
        $this->elevation = 0;
        $this->scale = 1.0;
        $this->projection = "ortho";
    }

    public function raDecToCartesian($raHours, $decDeg, $dist) {
        $raRad = $raHours * 15 * M_PI / 180;
        $decRad = $decDeg * M_PI / 180;
        $x = $dist * cos($decRad) * cos($raRad);
        $y = $dist * cos($decRad) * sin($raRad);
        $z = $dist * sin($decRad);
        return [$x, $y, $z];
    }

    public function rotate($points) {
        $az = $this->azimuth * M_PI / 180;
        $el = $this->elevation * M_PI / 180;
        $rotated = [];
        foreach ($points as $p) {
            list($x, $y, $z) = $p;
            // Y rotation
            $x1 = $x * cos($az) + $z * sin($az);
            $z1 = -$x * sin($az) + $z * cos($az);
            // X rotation
            $y2 = $y * cos($el) - $z1 * sin($el);
            $z2 = $y * sin($el) + $z1 * cos($el);
            $rotated[] = [$x1, $y2, $z2];
        }
        return $rotated;
    }

    public function project($points) {
        $screen = [];
        if ($this->projection == "persp") {
            $focal = 100;
            foreach ($points as $p) {
                list($x, $y, $z) = $p;
                if ($z > -$focal) {
                    $factor = $focal / ($z + $focal);
                    $screen[] = [$x * $factor * $this->scale, $y * $factor * $this->scale];
                } else {
                    $screen[] = [NAN, NAN];
                }
            }
        } else {
            foreach ($points as $p) {
                $screen[] = [$p[0] * $this->scale, $p[1] * $this->scale];
            }
        }
        return $screen;
    }

    public function drawLine(&$grid, $x0, $y0, $x1, $y1) {
        $dx = abs($x1 - $x0);
        $dy = abs($y1 - $y0);
        $sx = $x0 < $x1 ? 1 : -1;
        $sy = $y0 < $y1 ? 1 : -1;
        $err = $dx - $dy;
        while (true) {
            if ($x0 >= 0 && $x0 < count($grid[0]) && $y0 >= 0 && $y0 < count($grid)) {
                if ($grid[$y0][$x0] == " ") {
                    $grid[$y0][$x0] = "·";
                }
            }
            if ($x0 == $x1 && $y0 == $y1) break;
            $e2 = 2 * $err;
            if ($e2 > -$dy) { $err -= $dy; $x0 += $sx; }
            if ($e2 < $dx) { $err += $dx; $y0 += $sy; }
        }
    }

    public function drawASCII($screen, $connections, $width = 60, $height = 20) {
        $valid = [];
        foreach ($screen as $i => $p) {
            if (!is_nan($p[0])) {
                $valid[] = ['idx' => $i, 'x' => $p[0], 'y' => $p[1]];
            }
        }
        if (empty($valid)) {
            return "No stars visible from this angle.";
        }
        $minX = $valid[0]['x']; $maxX = $valid[0]['x'];
        $minY = $valid[0]['y']; $maxY = $valid[0]['y'];
        foreach ($valid as $v) {
            if ($v['x'] < $minX) $minX = $v['x'];
            if ($v['x'] > $maxX) $maxX = $v['x'];
            if ($v['y'] < $minY) $minY = $v['y'];
            if ($v['y'] > $maxY) $maxY = $v['y'];
        }
        $rangeX = $maxX - $minX ?: 1;
        $rangeY = $maxY - $minY ?: 1;
        $scaleX = ($width - 4) / $rangeX;
        $scaleY = ($height - 4) / $rangeY;
        $scale = min($scaleX, $scaleY);
        $grid = array_fill(0, $height, array_fill(0, $width, " "));
        $toGrid = function($x, $y) use ($minX, $minY, $scale) {
            return [(int)(($x - $minX) * $scale + 2), (int)(($y - $minY) * $scale + 2)];
        };
        $starPos = [];
        foreach ($valid as $v) {
            list($cx, $cy) = $toGrid($v['x'], $v['y']);
            if ($cx >= 0 && $cx < $width && $cy >= 0 && $cy < $height) {
                $grid[$cy][$cx] = "*";
                $starPos[$v['idx']] = [$cx, $cy];
            }
        }
        foreach ($connections as $conn) {
            list($i, $j) = $conn;
            if (isset($starPos[$i]) && isset($starPos[$j])) {
                list($x1, $y1) = $starPos[$i];
                list($x2, $y2) = $starPos[$j];
                $this->drawLine($grid, $x1, $y1, $x2, $y2);
            }
        }
        return implode("\n", array_map(function($row) { return implode("", $row); }, $grid));
    }

    public function view($name, $az = null, $el = null, $scale = null, $proj = null) {
        if ($az !== null) $this->azimuth = $az;
        if ($el !== null) $this->elevation = $el;
        if ($scale !== null) $this->scale = $scale;
        if ($proj !== null) $this->projection = $proj;
        global $CONSTELLATIONS;
        if (!isset($CONSTELLATIONS[$name])) {
            return "Constellation '$name' not found.";
        }
        $const = $CONSTELLATIONS[$name];
        $points = [];
        foreach ($const["stars"] as $star) {
            list($_, $ra, $dec, $dist) = $star;
            $points[] = $this->raDecToCartesian($ra, $dec, $dist);
        }
        $rotated = $this->rotate($points);
        $screen = $this->project($rotated);
        $ascii = $this->drawASCII($screen, $const["connections"]);
        $header = "\n🌟 Constellation: $name ({$const['abbr']})\n";
        $header .= "Azimuth: {$this->azimuth}°  Elevation: {$this->elevation}°  Scale: {$this->scale}\n";
        $header .= "Projection: {$this->projection}\n\n";
        return $header . $ascii;
    }
}

$opts = getopt("", ["view:", "list", "info:", "az:", "el:", "scale:", "projection:"]);

if (isset($opts["list"])) {
    echo "📋 Available Constellations:\n";
    foreach (array_keys($GLOBALS['CONSTELLATIONS']) as $name) {
        echo "$name ({$GLOBALS['CONSTELLATIONS'][$name]['abbr']})\n";
    }
    exit;
}

if (isset($opts["info"])) {
    $name = $opts["info"];
    if (!isset($GLOBALS['CONSTELLATIONS'][$name])) {
        echo "Constellation '$name' not found.\n";
        exit(1);
    }
    $c = $GLOBALS['CONSTELLATIONS'][$name];
    echo "\n✨ $name ({$c['abbr']})\n";
    echo "Mythology: {$c['mythology']}\n";
    echo "Notable Objects: {$c['notable_objects']}\n";
    exit;
}

if (isset($opts["view"])) {
    $name = $opts["view"];
    $az = isset($opts["az"]) ? (float)$opts["az"] : null;
    $el = isset($opts["el"]) ? (float)$opts["el"] : null;
    $scale = isset($opts["scale"]) ? (float)$opts["scale"] : null;
    $proj = $opts["projection"] ?? null;
    $map = new StarMap3D();
    echo $map->view($name, $az, $el, $scale, $proj);
    exit;
}

echo "Usage: php constellation_3d.php --view <constellation> [--az DEG] [--el DEG] [--scale FACTOR] [--projection ortho|persp]\n";
echo "       php constellation_3d.php --list\n";
echo "       php constellation_3d.php --info <constellation>\n";
?>
