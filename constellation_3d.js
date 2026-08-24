// constellation_3d.js
#!/usr/bin/env node
const { program } = require('commander');

const CONSTELLATIONS = {
    Orion: {
        abbr: "Ori",
        mythology: "Orion, a mighty hunter in Greek mythology, was placed among the stars by Zeus.",
        notable_objects: "Orion Nebula (M42), Horsehead Nebula",
        stars: [
            ["Betelgeuse", 5.919, 7.407, 130],
            ["Bellatrix", 5.250, 6.350, 243],
            ["Mintaka", 5.650, -0.300, 200],
            ["Alnilam", 5.630, -1.200, 408],
            ["Alnitak", 5.620, -1.950, 200],
            ["Saiph", 5.230, -9.670, 198],
            ["Rigel", 5.240, -8.200, 264],
        ],
        connections: [[0,1],[0,2],[0,3],[0,4],[0,5],[0,6],[1,2],[2,3],[3,4],[4,5],[5,6],[2,4]]
    },
    "Ursa Major": {
        abbr: "UMa",
        mythology: "Ursa Major represents the Great Bear. In Greek mythology, Callisto was transformed into a bear by Hera.",
        notable_objects: "M81, M82, Owl Nebula (M97)",
        stars: [
            ["Dubhe", 11.03, 61.75, 40],
            ["Merak", 11.02, 56.38, 24],
            ["Phecda", 11.85, 53.69, 28],
            ["Megrez", 12.15, 57.03, 22],
            ["Alioth", 12.90, 55.96, 24],
            ["Mizar", 13.23, 54.93, 23],
            ["Alkaid", 13.47, 49.31, 31],
        ],
        connections: [[0,1],[1,2],[2,3],[3,4],[4,5],[5,6]]
    },
    Cassiopeia: {
        abbr: "Cas",
        mythology: "Cassiopeia was the vain queen of Ethiopia who boasted about her beauty.",
        notable_objects: "Cassiopeia A (supernova remnant)",
        stars: [
            ["Segin", 0.77, 60.72, 94],
            ["Ruchbah", 1.43, 60.66, 26],
            ["Schedar", 0.57, 56.54, 69],
            ["Navi", 1.18, 55.54, 22],
            ["Caph", 0.46, 59.15, 20],
        ],
        connections: [[0,1],[1,2],[2,3],[3,4],[0,2],[2,4]]
    },
    Scorpius: {
        abbr: "Sco",
        mythology: "Scorpius represents the scorpion that killed Orion.",
        notable_objects: "Antares (red supergiant), Ptolemy's Cluster",
        stars: [
            ["Antares", 16.30, -26.43, 170],
            ["Graffias", 16.00, -22.62, 280],
            ["Dschubba", 16.00, -22.62, 280],
            ["Wei", 16.20, -25.00, 300],
            ["Shaula", 17.44, -37.05, 200],
        ],
        connections: [[0,1],[1,2],[2,3],[3,4]]
    },
    Lyra: {
        abbr: "Lyr",
        mythology: "Lyra represents the lyre of Orpheus, the legendary musician.",
        notable_objects: "Ring Nebula (M57), Vega",
        stars: [
            ["Vega", 18.62, 38.78, 7.7],
            ["Sheliak", 18.94, 33.36, 200],
            ["Sulafat", 18.50, 32.69, 200],
        ],
        connections: [[0,1],[0,2],[1,2]]
    }
};

class StarMap3D {
    constructor() {
        this.azimuth = 0;
        this.elevation = 0;
        this.scale = 1.0;
        this.projection = 'ortho';
    }

    raDecToCartesian(raHours, decDeg, dist) {
        const raRad = raHours * 15 * Math.PI / 180;
        const decRad = decDeg * Math.PI / 180;
        const x = dist * Math.cos(decRad) * Math.cos(raRad);
        const y = dist * Math.cos(decRad) * Math.sin(raRad);
        const z = dist * Math.sin(decRad);
        return [x, y, z];
    }

    rotate(points) {
        const az = this.azimuth * Math.PI / 180;
        const el = this.elevation * Math.PI / 180;
        return points.map(([x, y, z]) => {
            // Y rotation
            let x1 = x * Math.cos(az) + z * Math.sin(az);
            let z1 = -x * Math.sin(az) + z * Math.cos(az);
            // X rotation
            let y2 = y * Math.cos(el) - z1 * Math.sin(el);
            let z2 = y * Math.sin(el) + z1 * Math.cos(el);
            return [x1, y2, z2];
        });
    }

    project(points) {
        if (this.projection === 'persp') {
            const focal = 100;
            return points.map(([x, y, z]) => {
                if (z > -focal) {
                    const factor = focal / (z + focal);
                    return [x * factor * this.scale, y * factor * this.scale];
                } else {
                    return [NaN, NaN];
                }
            });
        } else {
            return points.map(([x, y]) => [x * this.scale, y * this.scale]);
        }
    }

    drawLine(grid, x0, y0, x1, y1) {
        const dx = Math.abs(x1 - x0);
        const dy = Math.abs(y1 - y0);
        const sx = x0 < x1 ? 1 : -1;
        const sy = y0 < y1 ? 1 : -1;
        let err = dx - dy;
        while (true) {
            if (x0 >= 0 && x0 < grid[0].length && y0 >= 0 && y0 < grid.length) {
                if (grid[y0][x0] === ' ') grid[y0][x0] = '·';
            }
            if (x0 === x1 && y0 === y1) break;
            const e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }

    drawASCII(screen, connections, width = 60, height = 20) {
        const valid = screen.map((p, i) => ({ idx: i, x: p[0], y: p[1] }))
                            .filter(p => !isNaN(p.x));
        if (valid.length === 0) return 'No stars visible from this angle.';
        let minX = valid[0].x, maxX = valid[0].x;
        let minY = valid[0].y, maxY = valid[0].y;
        for (const v of valid) {
            if (v.x < minX) minX = v.x;
            if (v.x > maxX) maxX = v.x;
            if (v.y < minY) minY = v.y;
            if (v.y > maxY) maxY = v.y;
        }
        const rangeX = maxX - minX || 1;
        const rangeY = maxY - minY || 1;
        const scaleX = (width - 4) / rangeX;
        const scaleY = (height - 4) / rangeY;
        const scale = Math.min(scaleX, scaleY);
        const grid = Array.from({ length: height }, () => Array(width).fill(' '));
        const toGrid = (x, y) => {
            const cx = Math.floor((x - minX) * scale + 2);
            const cy = Math.floor((y - minY) * scale + 2);
            return [cx, cy];
        };
        const starPos = {};
        for (const v of valid) {
            const [cx, cy] = toGrid(v.x, v.y);
            if (cx >= 0 && cx < width && cy >= 0 && cy < height) {
                grid[cy][cx] = '*';
                starPos[v.idx] = [cx, cy];
            }
        }
        for (const [i, j] of connections) {
            if (starPos[i] && starPos[j]) {
                const [x1, y1] = starPos[i];
                const [x2, y2] = starPos[j];
                this.drawLine(grid, x1, y1, x2, y2);
            }
        }
        return grid.map(row => row.join('')).join('\n');
    }

    view(name, az, el, scale, proj) {
        if (az !== undefined) this.azimuth = az;
        if (el !== undefined) this.elevation = el;
        if (scale !== undefined) this.scale = scale;
        if (proj !== undefined) this.projection = proj;
        const constData = CONSTELLATIONS[name];
        if (!constData) return `Constellation '${name}' not found.`;
        const points = constData.stars.map(([_, ra, dec, dist]) => this.raDecToCartesian(ra, dec, dist));
        const rotated = this.rotate(points);
        const screen = this.project(rotated);
        const ascii = this.drawASCII(screen, constData.connections);
        let header = `\n🌟 Constellation: ${name} (${constData.abbr})\n`;
        header += `Azimuth: ${this.azimuth}°  Elevation: ${this.elevation}°  Scale: ${this.scale}\n`;
        header += `Projection: ${this.projection}\n\n`;
        return header + ascii;
    }
}

program
    .command('view <constellation>')
    .option('--az <degrees>', 'Azimuth angle', parseFloat, 0)
    .option('--el <degrees>', 'Elevation angle', parseFloat, 0)
    .option('--scale <factor>', 'Zoom scale', parseFloat, 1.0)
    .option('--projection <type>', 'Projection type (ortho/persp)', 'ortho')
    .action((constellation, options) => {
        const map = new StarMap3D();
        console.log(map.view(constellation, options.az, options.el, options.scale, options.projection));
    });

program
    .command('list')
    .action(() => {
        console.log('📋 Available Constellations:');
        for (const name of Object.keys(CONSTELLATIONS)) {
            console.log(`${name} (${CONSTELLATIONS[name].abbr})`);
        }
    });

program
    .command('info <constellation>')
    .action((name) => {
        const data = CONSTELLATIONS[name];
        if (!data) {
            console.log(`Constellation '${name}' not found.`);
            return;
        }
        console.log(`\n✨ ${name} (${data.abbr})`);
        console.log(`Mythology: ${data.mythology}`);
        console.log(`Notable Objects: ${data.notable_objects}`);
    });

program.parse(process.argv);
