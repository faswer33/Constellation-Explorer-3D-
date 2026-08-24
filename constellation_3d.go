// constellation_3d.go
package main

import (
	"encoding/json"
	"flag"
	"fmt"
	"math"
	"os"
	"strings"
)

type ConstellationData struct {
	Abbr            string          `json:"abbr"`
	Mythology       string          `json:"mythology"`
	NotableObjects  string          `json:"notable_objects"`
	Stars           [][4]interface{} `json:"stars"` // name, ra, dec, dist
	Connections     [][2]int        `json:"connections"`
}

var constellations = map[string]ConstellationData{
	"Orion": {
		Abbr:           "Ori",
		Mythology:      "Orion, a mighty hunter in Greek mythology, was placed among the stars by Zeus.",
		NotableObjects: "Orion Nebula (M42), Horsehead Nebula",
		Stars: [][4]interface{}{
			{"Betelgeuse", 5.919, 7.407, 130.0},
			{"Bellatrix", 5.250, 6.350, 243.0},
			{"Mintaka", 5.650, -0.300, 200.0},
			{"Alnilam", 5.630, -1.200, 408.0},
			{"Alnitak", 5.620, -1.950, 200.0},
			{"Saiph", 5.230, -9.670, 198.0},
			{"Rigel", 5.240, -8.200, 264.0},
		},
		Connections: [][2]int{{0, 1}, {0, 2}, {0, 3}, {0, 4}, {0, 5}, {0, 6}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {2, 4}},
	},
	"Ursa Major": {
		Abbr:           "UMa",
		Mythology:      "Ursa Major represents the Great Bear. In Greek mythology, Callisto was transformed into a bear by Hera.",
		NotableObjects: "M81, M82, Owl Nebula (M97)",
		Stars: [][4]interface{}{
			{"Dubhe", 11.03, 61.75, 40.0},
			{"Merak", 11.02, 56.38, 24.0},
			{"Phecda", 11.85, 53.69, 28.0},
			{"Megrez", 12.15, 57.03, 22.0},
			{"Alioth", 12.90, 55.96, 24.0},
			{"Mizar", 13.23, 54.93, 23.0},
			{"Alkaid", 13.47, 49.31, 31.0},
		},
		Connections: [][2]int{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}},
	},
	"Cassiopeia": {
		Abbr:           "Cas",
		Mythology:      "Cassiopeia was the vain queen of Ethiopia who boasted about her beauty.",
		NotableObjects: "Cassiopeia A (supernova remnant)",
		Stars: [][4]interface{}{
			{"Segin", 0.77, 60.72, 94.0},
			{"Ruchbah", 1.43, 60.66, 26.0},
			{"Schedar", 0.57, 56.54, 69.0},
			{"Navi", 1.18, 55.54, 22.0},
			{"Caph", 0.46, 59.15, 20.0},
		},
		Connections: [][2]int{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {0, 2}, {2, 4}},
	},
	"Scorpius": {
		Abbr:           "Sco",
		Mythology:      "Scorpius represents the scorpion that killed Orion.",
		NotableObjects: "Antares (red supergiant), Ptolemy's Cluster",
		Stars: [][4]interface{}{
			{"Antares", 16.30, -26.43, 170.0},
			{"Graffias", 16.00, -22.62, 280.0},
			{"Dschubba", 16.00, -22.62, 280.0},
			{"Wei", 16.20, -25.00, 300.0},
			{"Shaula", 17.44, -37.05, 200.0},
		},
		Connections: [][2]int{{0, 1}, {1, 2}, {2, 3}, {3, 4}},
	},
	"Lyra": {
		Abbr:           "Lyr",
		Mythology:      "Lyra represents the lyre of Orpheus, the legendary musician.",
		NotableObjects: "Ring Nebula (M57), Vega",
		Stars: [][4]interface{}{
			{"Vega", 18.62, 38.78, 7.7},
			{"Sheliak", 18.94, 33.36, 200.0},
			{"Sulafat", 18.50, 32.69, 200.0},
		},
		Connections: [][2]int{{0, 1}, {0, 2}, {1, 2}},
	},
}

type StarMap3D struct {
	azimuth    float64
	elevation  float64
	scale      float64
	projection string
}

func NewStarMap3D() *StarMap3D {
	return &StarMap3D{azimuth: 0, elevation: 0, scale: 1.0, projection: "ortho"}
}

func (s *StarMap3D) raDecToCartesian(raHours, decDeg, dist float64) (float64, float64, float64) {
	raRad := raHours * 15.0 * math.Pi / 180.0
	decRad := decDeg * math.Pi / 180.0
	x := dist * math.Cos(decRad) * math.Cos(raRad)
	y := dist * math.Cos(decRad) * math.Sin(raRad)
	z := dist * math.Sin(decRad)
	return x, y, z
}

func (s *StarMap3D) rotate(points [][3]float64) [][3]float64 {
	az := s.azimuth * math.Pi / 180.0
	el := s.elevation * math.Pi / 180.0
	rotated := make([][3]float64, len(points))
	for i, p := range points {
		x, y, z := p[0], p[1], p[2]
		// Y rotation (azimuth)
		x1 := x*math.Cos(az) + z*math.Sin(az)
		z1 := -x*math.Sin(az) + z*math.Cos(az)
		// X rotation (elevation)
		y2 := y*math.Cos(el) - z1*math.Sin(el)
		z2 := y*math.Sin(el) + z1*math.Cos(el)
		rotated[i] = [3]float64{x1, y2, z2}
	}
	return rotated
}

func (s *StarMap3D) project(points [][3]float64) [][2]float64 {
	screen := make([][2]float64, len(points))
	if s.projection == "persp" {
		focal := 100.0
		for i, p := range points {
			x, y, z := p[0], p[1], p[2]
			if z > -focal {
				factor := focal / (z + focal)
				screen[i] = [2]float64{x * factor * s.scale, y * factor * s.scale}
			} else {
				screen[i] = [2]float64{math.NaN(), math.NaN()}
			}
		}
	} else {
		for i, p := range points {
			screen[i] = [2]float64{p[0] * s.scale, p[1] * s.scale}
		}
	}
	return screen
}

func (s *StarMap3D) drawLine(grid [][]string, x0, y0, x1, y1 int) {
	dx := abs(x1 - x0)
	dy := abs(y1 - y0)
	sx := 1
	if x0 > x1 {
		sx = -1
	}
	sy := 1
	if y0 > y1 {
		sy = -1
	}
	err := dx - dy
	for {
		if x0 >= 0 && x0 < len(grid[0]) && y0 >= 0 && y0 < len(grid) {
			if grid[y0][x0] == " " {
				grid[y0][x0] = "·"
			}
		}
		if x0 == x1 && y0 == y1 {
			break
		}
		e2 := 2 * err
		if e2 > -dy {
			err -= dy
			x0 += sx
		}
		if e2 < dx {
			err += dx
			y0 += sy
		}
	}
}

func abs(a int) int {
	if a < 0 {
		return -a
	}
	return a
}

func (s *StarMap3D) drawASCII(screen [][2]float64, connections [][2]int, width, height int) string {
	// Filter valid points
	type validPoint struct {
		idx int
		x   float64
		y   float64
	}
	var valid []validPoint
	for i, p := range screen {
		if !math.IsNaN(p[0]) {
			valid = append(valid, validPoint{i, p[0], p[1]})
		}
	}
	if len(valid) == 0 {
		return "No stars visible from this angle."
	}
	// Bounds
	minX, maxX := valid[0].x, valid[0].x
	minY, maxY := valid[0].y, valid[0].y
	for _, v := range valid {
		if v.x < minX {
			minX = v.x
		}
		if v.x > maxX {
			maxX = v.x
		}
		if v.y < minY {
			minY = v.y
		}
		if v.y > maxY {
			maxY = v.y
		}
	}
	rangeX := maxX - minX
	rangeY := maxY - minY
	if rangeX == 0 {
		rangeX = 1
	}
	if rangeY == 0 {
		rangeY = 1
	}
	scaleX := float64(width-4) / rangeX
	scaleY := float64(height-4) / rangeY
	scale := scaleX
	if scaleY < scale {
		scale = scaleY
	}
	grid := make([][]string, height)
	for i := range grid {
		grid[i] = make([]string, width)
		for j := range grid[i] {
			grid[i][j] = " "
		}
	}
	toGrid := func(x, y float64) (int, int) {
		cx := int((x-minX)*scale + 2)
		cy := int((y-minY)*scale + 2)
		return cx, cy
	}
	starPos := make(map[int][2]int)
	for _, v := range valid {
		cx, cy := toGrid(v.x, v.y)
		if cx >= 0 && cx < width && cy >= 0 && cy < height {
			grid[cy][cx] = "*"
			starPos[v.idx] = [2]int{cx, cy}
		}
	}
	for _, conn := range connections {
		p1, ok1 := starPos[conn[0]]
		p2, ok2 := starPos[conn[1]]
		if ok1 && ok2 {
			s.drawLine(grid, p1[0], p1[1], p2[0], p2[1])
		}
	}
	// Build string
	var sb strings.Builder
	for _, row := range grid {
		sb.WriteString(strings.Join(row, ""))
		sb.WriteByte('\n')
	}
	return sb.String()
}

func (s *StarMap3D) view(name string, az, el, scale *float64, proj *string) string {
	if az != nil {
		s.azimuth = *az
	}
	if el != nil {
		s.elevation = *el
	}
	if scale != nil {
		s.scale = *scale
	}
	if proj != nil {
		s.projection = *proj
	}
	constData, ok := constellations[name]
	if !ok {
		return "Constellation '" + name + "' not found."
	}
	// Get star points
	points := make([][3]float64, len(constData.Stars))
	for i, star := range constData.Stars {
		ra := star[1].(float64)
		dec := star[2].(float64)
		dist := star[3].(float64)
		x, y, z := s.raDecToCartesian(ra, dec, dist)
		points[i] = [3]float64{x, y, z}
	}
	rotated := s.rotate(points)
	screen := s.project(rotated)
	ascii := s.drawASCII(screen, constData.Connections, 60, 20)
	header := fmt.Sprintf("\n🌟 Constellation: %s (%s)\n", name, constData.Abbr)
	header += fmt.Sprintf("Azimuth: %.1f°  Elevation: %.1f°  Scale: %.1f\n", s.azimuth, s.elevation, s.scale)
	header += fmt.Sprintf("Projection: %s\n\n", s.projection)
	return header + ascii
}

func list() string {
	var sb strings.Builder
	sb.WriteString("📋 Available Constellations:\n")
	for name, data := range constellations {
		sb.WriteString(fmt.Sprintf("%s (%s)\n", name, data.Abbr))
	}
	return sb.String()
}

func info(name string) string {
	data, ok := constellations[name]
	if !ok {
		return "Constellation '" + name + "' not found."
	}
	return fmt.Sprintf("\n✨ %s (%s)\nMythology: %s\nNotable Objects: %s\n", name, data.Abbr, data.Mythology, data.NotableObjects)
}

func main() {
	if len(os.Args) < 2 {
		fmt.Println("Usage: constellation_3d <command> [options]")
		return
	}
	switch os.Args[1] {
	case "view":
		viewCmd := flag.NewFlagSet("view", flag.ExitOnError)
		constName := viewCmd.String("const", "", "Constellation name")
		az := viewCmd.Float64("az", 0.0, "Azimuth angle (degrees)")
		el := viewCmd.Float64("el", 0.0, "Elevation angle (degrees)")
		scale := viewCmd.Float64("scale", 1.0, "Zoom scale")
		proj := viewCmd.String("projection", "ortho", "Projection type (ortho/persp)")
		viewCmd.Parse(os.Args[2:])
		if *constName == "" && len(viewCmd.Args()) > 0 {
			*constName = viewCmd.Args()[0]
		}
		if *constName == "" {
			fmt.Println("view requires a constellation name")
			return
		}
		s := NewStarMap3D()
		fmt.Print(s.view(*constName, az, el, scale, proj))
	case "list":
		fmt.Println(list())
	case "info":
		if len(os.Args) < 3 {
			fmt.Println("info <constellation>")
			return
		}
		fmt.Print(info(os.Args[2]))
	default:
		fmt.Println("Unknown command. Use view, list, info.")
	}
}
