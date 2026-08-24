# constellation_3d.py
import sys
import json
import math
import argparse
from typing import List, Dict, Tuple

# Constellation data: name, abbreviation, mythology, notable objects, stars (name, ra_hours, dec_degrees, distance_pc), connections (indices)
CONSTELLATIONS = {
    "Orion": {
        "abbr": "Ori",
        "mythology": "Orion, a mighty hunter in Greek mythology, was placed among the stars by Zeus.",
        "notable_objects": "Orion Nebula (M42), Horsehead Nebula",
        "stars": [
            ("Betelgeuse", 5.919, 7.407, 130),
            ("Bellatrix", 5.250, 6.350, 243),
            ("Mintaka", 5.650, -0.300, 200),
            ("Alnilam", 5.630, -1.200, 408),
            ("Alnitak", 5.620, -1.950, 200),
            ("Saiph", 5.230, -9.670, 198),
            ("Rigel", 5.240, -8.200, 264),
        ],
        "connections": [(0,1), (0,2), (0,3), (0,4), (0,5), (0,6), (1,2), (2,3), (3,4), (4,5), (5,6), (2,4)]
    },
    "Ursa Major": {
        "abbr": "UMa",
        "mythology": "Ursa Major represents the Great Bear. In Greek mythology, Callisto was transformed into a bear by Hera.",
        "notable_objects": "M81, M82, Owl Nebula (M97)",
        "stars": [
            ("Dubhe", 11.03, 61.75, 40),
            ("Merak", 11.02, 56.38, 24),
            ("Phecda", 11.85, 53.69, 28),
            ("Megrez", 12.15, 57.03, 22),
            ("Alioth", 12.90, 55.96, 24),
            ("Mizar", 13.23, 54.93, 23),
            ("Alkaid", 13.47, 49.31, 31),
        ],
        "connections": [(0,1), (1,2), (2,3), (3,4), (4,5), (5,6)]
    },
    "Cassiopeia": {
        "abbr": "Cas",
        "mythology": "Cassiopeia was the vain queen of Ethiopia who boasted about her beauty.",
        "notable_objects": "Cassiopeia A (supernova remnant)",
        "stars": [
            ("Segin", 0.77, 60.72, 94),
            ("Ruchbah", 1.43, 60.66, 26),
            ("Schedar", 0.57, 56.54, 69),
            ("Navi", 1.18, 55.54, 22),
            ("Caph", 0.46, 59.15, 20),
        ],
        "connections": [(0,1), (1,2), (2,3), (3,4), (0,2), (2,4)]
    },
    "Scorpius": {
        "abbr": "Sco",
        "mythology": "Scorpius represents the scorpion that killed Orion.",
        "notable_objects": "Antares (red supergiant), Ptolemy's Cluster",
        "stars": [
            ("Antares", 16.30, -26.43, 170),
            ("Graffias", 16.00, -22.62, 280),
            ("Dschubba", 16.00, -22.62, 280),
            ("Wei", 16.20, -25.00, 300),
            ("Shaula", 17.44, -37.05, 200),
        ],
        "connections": [(0,1), (1,2), (2,3), (3,4)]
    },
    "Lyra": {
        "abbr": "Lyr",
        "mythology": "Lyra represents the lyre of Orpheus, the legendary musician.",
        "notable_objects": "Ring Nebula (M57), Vega",
        "stars": [
            ("Vega", 18.62, 38.78, 7.7),
            ("Sheliak", 18.94, 33.36, 200),
            ("Sulafat", 18.50, 32.69, 200),
        ],
        "connections": [(0,1), (0,2), (1,2)]
    }
}

class StarMap3D:
    def __init__(self):
        self.constellations = CONSTELLATIONS
        self.azimuth = 0.0  # degrees
        self.elevation = 0.0
        self.scale = 1.0
        self.projection = "ortho"  # or "persp"

    def ra_dec_to_cartesian(self, ra_hours, dec_deg, distance_pc):
        """Convert RA (hours), Dec (degrees), distance (pc) to Cartesian (x,y,z)."""
        ra_rad = math.radians(ra_hours * 15.0)
        dec_rad = math.radians(dec_deg)
        x = distance_pc * math.cos(dec_rad) * math.cos(ra_rad)
        y = distance_pc * math.cos(dec_rad) * math.sin(ra_rad)
        z = distance_pc * math.sin(dec_rad)
        return x, y, z

    def rotate(self, points, az_deg, el_deg):
        """Rotate points by azimuth (around y-axis) then elevation (around x-axis)."""
        az = math.radians(az_deg)
        el = math.radians(el_deg)
        rotated = []
        for x, y, z in points:
            # Rotate around Y (azimuth)
            x1 = x * math.cos(az) + z * math.sin(az)
            z1 = -x * math.sin(az) + z * math.cos(az)
            # Rotate around X (elevation)
            y2 = y * math.cos(el) - z1 * math.sin(el)
            z2 = y * math.sin(el) + z1 * math.cos(el)
            rotated.append((x1, y2, z2))
        return rotated

    def project(self, points):
        """Project 3D points to 2D screen coordinates."""
        if self.projection == "persp":
            # Simple perspective (z as depth)
            focal_length = 100.0
            screen = []
            for x, y, z in points:
                if z > -focal_length:
                    factor = focal_length / (z + focal_length)
                    sx = x * factor * self.scale
                    sy = y * factor * self.scale
                    screen.append((sx, sy))
                else:
                    screen.append((None, None))
            return screen
        else:
            # Orthographic
            return [(x * self.scale, y * self.scale) for x, y, _ in points]

    def draw_ascii(self, screen_points, connections, width=60, height=20):
        """Draw constellation on ASCII grid."""
        # Filter out points that are None (behind camera)
        valid = [(i, p) for i, p in enumerate(screen_points) if p[0] is not None]
        if not valid:
            return "No stars visible from this angle."

        # Find bounds
        xs = [p[0] for _, p in valid]
        ys = [p[1] for _, p in valid]
        if not xs:
            return "No stars visible."
        min_x, max_x = min(xs), max(xs)
        min_y, max_y = min(ys), max(ys)

        # Add padding
        range_x = max_x - min_x or 1
        range_y = max_y - min_y or 1
        # Scale to fit grid (keep aspect ratio)
        scale_x = (width - 4) / range_x
        scale_y = (height - 4) / range_y
        scale = min(scale_x, scale_y)

        grid = [[' ' for _ in range(width)] for _ in range(height)]

        def to_grid(x, y):
            cx = int((x - min_x) * scale + 2)
            cy = int((y - min_y) * scale + 2)
            return cx, cy

        # Place stars
        star_pos = {}
        for idx, (sx, sy) in valid:
            cx, cy = to_grid(sx, sy)
            if 0 <= cx < width and 0 <= cy < height:
                grid[cy][cx] = '*'
                star_pos[idx] = (cx, cy)

        # Draw connections
        for i, j in connections:
            if i in star_pos and j in star_pos:
                x1, y1 = star_pos[i]
                x2, y2 = star_pos[j]
                self.draw_line(grid, x1, y1, x2, y2)

        # Convert grid to string
        return '\n'.join(''.join(row) for row in grid)

    def draw_line(self, grid, x0, y0, x1, y1):
        """Bresenham line drawing algorithm."""
        dx = abs(x1 - x0)
        dy = abs(y1 - y0)
        sx = 1 if x0 < x1 else -1
        sy = 1 if y0 < y1 else -1
        err = dx - dy
        while True:
            if 0 <= x0 < len(grid[0]) and 0 <= y0 < len(grid):
                if grid[y0][x0] == ' ':
                    grid[y0][x0] = '·'
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 > -dy:
                err -= dy
                x0 += sx
            if e2 < dx:
                err += dx
                y0 += sy

    def get_star_data(self, constellation_name):
        """Get star positions as 3D points and connections."""
        const = self.constellations.get(constellation_name)
        if not const:
            return None, None
        stars = const["stars"]
        connections = const["connections"]
        points = []
        for name, ra, dec, dist in stars:
            x, y, z = self.ra_dec_to_cartesian(ra, dec, dist)
            points.append((x, y, z))
        return points, connections

    def view(self, constellation_name, az=None, el=None, scale=None, projection=None):
        if az is not None:
            self.azimuth = az
        if el is not None:
            self.elevation = el
        if scale is not None:
            self.scale = scale
        if projection is not None:
            self.projection = projection

        points, connections = self.get_star_data(constellation_name)
        if points is None:
            return f"Constellation '{constellation_name}' not found."

        rotated = self.rotate(points, self.azimuth, self.elevation)
        screen = self.project(rotated)
        ascii_art = self.draw_ascii(screen, connections)
        info = self.constellations[constellation_name]
        header = f"\n🌟 Constellation: {constellation_name} ({info['abbr']})\n"
        header += f"Azimuth: {self.azimuth}°  Elevation: {self.elevation}°  Scale: {self.scale}\n"
        header += f"Projection: {self.projection}\n\n"
        return header + ascii_art

    def list_constellations(self):
        return "\n".join(f"{name} ({data['abbr']})" for name, data in self.constellations.items())

    def info(self, name):
        data = self.constellations.get(name)
        if not data:
            return f"Constellation '{name}' not found."
        return f"\n✨ {name} ({data['abbr']})\nMythology: {data['mythology']}\nNotable Objects: {data['notable_objects']}\n"

def main():
    parser = argparse.ArgumentParser(description="Constellation Explorer 3D")
    subparsers = parser.add_subparsers(dest="cmd", required=True)

    view_parser = subparsers.add_parser("view")
    view_parser.add_argument("constellation")
    view_parser.add_argument("--az", type=float, default=0.0, help="Azimuth angle (degrees)")
    view_parser.add_argument("--el", type=float, default=0.0, help="Elevation angle (degrees)")
    view_parser.add_argument("--scale", type=float, default=1.0, help="Zoom scale")
    view_parser.add_argument("--projection", choices=["ortho", "persp"], default="ortho", help="Projection type")

    subparsers.add_parser("list")
    info_parser = subparsers.add_parser("info")
    info_parser.add_argument("constellation")

    args = parser.parse_args()
    map3d = StarMap3D()

    if args.cmd == "view":
        result = map3d.view(args.constellation, args.az, args.el, args.scale, args.projection)
        print(result)
    elif args.cmd == "list":
        print("📋 Available Constellations:")
        print(map3d.list_constellations())
    elif args.cmd == "info":
        print(map3d.info(args.constellation))

if __name__ == "__main__":
    main()
