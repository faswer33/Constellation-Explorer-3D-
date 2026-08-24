// constellation_3d.cpp
#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <map>
#include <cmath>
#include <algorithm>
#include <random>
#include <nlohmann/json.hpp>
#include <getopt.h>

using namespace std;
using json = nlohmann::json;

struct ConstellationData {
    string abbr;
    string mythology;
    string notable_objects;
    vector<array<double, 4>> stars; // name index not stored, just ra, dec, dist
    vector<pair<int, int>> connections;
};

map<string, ConstellationData> constellations = {
    {"Orion", {
        "Ori",
        "Orion, a mighty hunter in Greek mythology, was placed among the stars by Zeus.",
        "Orion Nebula (M42), Horsehead Nebula",
        {{5.919, 7.407, 130}, {5.250, 6.350, 243}, {5.650, -0.300, 200},
         {5.630, -1.200, 408}, {5.620, -1.950, 200}, {5.230, -9.670, 198},
         {5.240, -8.200, 264}},
        {{0,1},{0,2},{0,3},{0,4},{0,5},{0,6},{1,2},{2,3},{3,4},{4,5},{5,6},{2,4}}
    }},
    {"Ursa Major", {
        "UMa",
        "Ursa Major represents the Great Bear. In Greek mythology, Callisto was transformed into a bear by Hera.",
        "M81, M82, Owl Nebula (M97)",
        {{11.03, 61.75, 40}, {11.02, 56.38, 24}, {11.85, 53.69, 28},
         {12.15, 57.03, 22}, {12.90, 55.96, 24}, {13.23, 54.93, 23},
         {13.47, 49.31, 31}},
        {{0,1},{1,2},{2,3},{3,4},{4,5},{5,6}}
    }},
    {"Cassiopeia", {
        "Cas",
        "Cassiopeia was the vain queen of Ethiopia who boasted about her beauty.",
        "Cassiopeia A (supernova remnant)",
        {{0.77, 60.72, 94}, {1.43, 60.66, 26}, {0.57, 56.54, 69},
         {1.18, 55.54, 22}, {0.46, 59.15, 20}},
        {{0,1},{1,2},{2,3},{3,4},{0,2},{2,4}}
    }},
    {"Scorpius", {
        "Sco",
        "Scorpius represents the scorpion that killed Orion.",
        "Antares (red supergiant), Ptolemy's Cluster",
        {{16.30, -26.43, 170}, {16.00, -22.62, 280}, {16.00, -22.62, 280},
         {16.20, -25.00, 300}, {17.44, -37.05, 200}},
        {{0,1},{1,2},{2,3},{3,4}}
    }},
    {"Lyra", {
        "Lyr",
        "Lyra represents the lyre of Orpheus, the legendary musician.",
        "Ring Nebula (M57), Vega",
        {{18.62, 38.78, 7.7}, {18.94, 33.36, 200}, {18.50, 32.69, 200}},
        {{0,1},{0,2},{1,2}}
    }}
};

class StarMap3D {
public:
    double azimuth = 0;
    double elevation = 0;
    double scale = 1.0;
    string projection = "ortho";

    array<double, 3> raDecToCartesian(double raHours, double decDeg, double dist) {
        double raRad = raHours * 15 * M_PI / 180;
        double decRad = decDeg * M_PI / 180;
        double x = dist * cos(decRad) * cos(raRad);
        double y = dist * cos(decRad) * sin(raRad);
        double z = dist * sin(decRad);
        return {x, y, z};
    }

    vector<array<double, 3>> rotatePoints(const vector<array<double, 3>>& points) {
        double az = azimuth * M_PI / 180;
        double el = elevation * M_PI / 180;
        vector<array<double, 3>> rotated;
        for (auto& p : points) {
            double x = p[0], y = p[1], z = p[2];
            double x1 = x * cos(az) + z * sin(az);
            double z1 = -x * sin(az) + z * cos(az);
            double y2 = y * cos(el) - z1 * sin(el);
            double z2 = y * sin(el) + z1 * cos(el);
            rotated.push_back({x1, y2, z2});
        }
        return rotated;
    }

    vector<array<double, 2>> projectPoints(const vector<array<double, 3>>& points) {
        vector<array<double, 2>> screen;
        if (projection == "persp") {
            double focal = 100;
            for (auto& p : points) {
                double x = p[0], y = p[1], z = p[2];
                if (z > -focal) {
                    double factor = focal / (z + focal);
                    screen.push_back({x * factor * scale, y * factor * scale});
                } else {
                    screen.push_back({NAN, NAN});
                }
            }
        } else {
            for (auto& p : points) {
                screen.push_back({p[0] * scale, p[1] * scale});
            }
        }
        return screen;
    }

    void drawLine(vector<vector<char>>& grid, int x0, int y0, int x1, int y1) {
        int dx = abs(x1 - x0);
        int dy = abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            if (x0 >= 0 && x0 < (int)grid[0].size() && y0 >= 0 && y0 < (int)grid.size()) {
                if (grid[y0][x0] == ' ') grid[y0][x0] = '·';
            }
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }

    string drawASCII(const vector<array<double, 2>>& screen, const vector<pair<int, int>>& connections, int width, int height) {
        struct ValidPoint { int idx; double x; double y; };
        vector<ValidPoint> valid;
        for (int i = 0; i < (int)screen.size(); i++) {
            if (!isnan(screen[i][0])) {
                valid.push_back({i, screen[i][0], screen[i][1]});
            }
        }
        if (valid.empty()) return "No stars visible from this angle.";
        double minX = valid[0].x, maxX = valid[0].x;
        double minY = valid[0].y, maxY = valid[0].y;
        for (auto& v : valid) {
            if (v.x < minX) minX = v.x;
            if (v.x > maxX) maxX = v.x;
            if (v.y < minY) minY = v.y;
            if (v.y > maxY) maxY = v.y;
        }
        double rangeX = maxX - minX != 0 ? maxX - minX : 1;
        double rangeY = maxY - minY != 0 ? maxY - minY : 1;
        double scaleX = (width - 4) / rangeX;
        double scaleY = (height - 4) / rangeY;
        double scale = min(scaleX, scaleY);
        vector<vector<char>> grid(height, vector<char>(width, ' '));
        auto toGrid = [&](double x, double y) -> pair<int,int> {
            int cx = (int)((x - minX) * scale + 2);
            int cy = (int)((y - minY) * scale + 2);
            return {cx, cy};
        };
        map<int, pair<int,int>> starPos;
        for (auto& v : valid) {
            auto [cx, cy] = toGrid(v.x, v.y);
            if (cx >= 0 && cx < width && cy >= 0 && cy < height) {
                grid[cy][cx] = '*';
                starPos[v.idx] = {cx, cy};
            }
        }
        for (auto& conn : connections) {
            if (starPos.count(conn.first) && starPos.count(conn.second)) {
                auto [x1, y1] = starPos[conn.first];
                auto [x2, y2] = starPos[conn.second];
                drawLine(grid, x1, y1, x2, y2);
            }
        }
        string result;
        for (auto& row : grid) {
            result += string(row.begin(), row.end()) + "\n";
        }
        return result;
    }

    string view(const string& name, double* az, double* el, double* sc, const string* proj) {
        if (az) azimuth = *az;
        if (el) elevation = *el;
        if (sc) scale = *sc;
        if (proj) projection = *proj;

        if (!constellations.count(name)) {
            return "Constellation '" + name + "' not found.";
        }
        auto& constData = constellations[name];
        vector<array<double, 3>> points;
        for (auto& star : constData.stars) {
            points.push_back(raDecToCartesian(star[0], star[1], star[2]));
        }
        auto rotated = rotatePoints(points);
        auto screen = projectPoints(rotated);
        string ascii = drawASCII(screen, constData.connections, 60, 20);
        string header = "\n🌟 Constellation: " + name + " (" + constData.abbr + ")\n";
        char buf[100];
        snprintf(buf, sizeof(buf), "Azimuth: %.1f°  Elevation: %.1f°  Scale: %.1f\n", azimuth, elevation, scale);
        header += buf;
        header += "Projection: " + projection + "\n\n";
        return header + ascii;
    }

    static string listConstellations() {
        string result = "📋 Available Constellations:\n";
        for (auto& kv : constellations) {
            result += kv.first + " (" + kv.second.abbr + ")\n";
        }
        return result;
    }

    static string info(const string& name) {
        if (!constellations.count(name)) {
            return "Constellation '" + name + "' not found.";
        }
        auto& data = constellations[name];
        return "\n✨ " + name + " (" + data.abbr + ")\nMythology: " + data.mythology + "\nNotable Objects: " + data.notable_objects + "\n";
    }
};

int main(int argc, char* argv[]) {
    if (argc < 2) {
        cerr << "Usage: constellation_3d <command> [options]\n";
        return 1;
    }
    string cmd = argv[1];
    StarMap3D map;

    if (cmd == "view") {
        string name;
        double az = NAN, el = NAN, scale = NAN;
        string proj;
        for (int i=2; i<argc; i++) {
            string arg = argv[i];
            if (arg == "--const" && i+1 < argc) name = argv[++i];
            else if (arg == "--az" && i+1 < argc) az = stod(argv[++i]);
            else if (arg == "--el" && i+1 < argc) el = stod(argv[++i]);
            else if (arg == "--scale" && i+1 < argc) scale = stod(argv[++i]);
            else if (arg == "--projection" && i+1 < argc) proj = argv[++i];
            else if (arg[0] != '-') name = arg;
        }
        if (name.empty()) {
            cerr << "view requires a constellation name\n";
            return 1;
        }
        double* azPtr = isnan(az) ? nullptr : &az;
        double* elPtr = isnan(el) ? nullptr : &el;
        double* scalePtr = isnan(scale) ? nullptr : &scale;
        const string* projPtr = proj.empty() ? nullptr : &proj;
        cout << map.view(name, azPtr, elPtr, scalePtr, projPtr);
    } else if (cmd == "list") {
        cout << StarMap3D::listConstellations();
    } else if (cmd == "info") {
        string name;
        for (int i=2; i<argc; i++) {
            if (string(argv[i]) != "--const") name = argv[i];
        }
        if (name.empty()) {
            cerr << "info requires a constellation name\n";
            return 1;
        }
        cout << StarMap3D::info(name);
    } else {
        cerr << "Unknown command. Use view, list, info.\n";
        return 1;
    }
    return 0;
}
