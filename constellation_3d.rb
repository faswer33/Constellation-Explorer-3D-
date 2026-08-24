# constellation_3d.rb
#!/usr/bin/env ruby
require 'json'
require 'optparse'
require 'date'

CONSTELLATIONS = {
  "Orion" => {
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
  "Ursa Major" => {
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
  "Cassiopeia" => {
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
  "Scorpius" => {
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
  "Lyra" => {
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
}

class StarMap3D
  attr_accessor :azimuth, :elevation, :scale, :projection

  def initialize
    @azimuth = 0
    @elevation = 0
    @scale = 1.0
    @projection = "ortho"
  end

  def ra_dec_to_cartesian(ra_hours, dec_deg, dist)
    ra_rad = ra_hours * 15 * Math::PI / 180
    dec_rad = dec_deg * Math::PI / 180
    x = dist * Math.cos(dec_rad) * Math.cos(ra_rad)
    y = dist * Math.cos(dec_rad) * Math.sin(ra_rad)
    z = dist * Math.sin(dec_rad)
    [x, y, z]
  end

  def rotate(points)
    az = @azimuth * Math::PI / 180
    el = @elevation * Math::PI / 180
    points.map do |x, y, z|
      # Y rotation
      x1 = x * Math.cos(az) + z * Math.sin(az)
      z1 = -x * Math.sin(az) + z * Math.cos(az)
      # X rotation
      y2 = y * Math.cos(el) - z1 * Math.sin(el)
      z2 = y * Math.sin(el) + z1 * Math.cos(el)
      [x1, y2, z2]
    end
  end

  def project(points)
    if @projection == "persp"
      focal = 100
      points.map do |x, y, z|
        if z > -focal
          factor = focal / (z + focal)
          [x * factor * @scale, y * factor * @scale]
        else
          [Float::NAN, Float::NAN]
        end
      end
    else
      points.map { |x, y, _| [x * @scale, y * @scale] }
    end
  end

  def draw_line(grid, x0, y0, x1, y1)
    dx = (x1 - x0).abs
    dy = (y1 - y0).abs
    sx = x0 < x1 ? 1 : -1
    sy = y0 < y1 ? 1 : -1
    err = dx - dy
    loop do
      if x0 >= 0 && x0 < grid[0].size && y0 >= 0 && y0 < grid.size
        if grid[y0][x0] == " "
          grid[y0][x0] = "·"
        end
      end
      break if x0 == x1 && y0 == y1
      e2 = 2 * err
      if e2 > -dy
        err -= dy
        x0 += sx
      end
      if e2 < dx
        err += dx
        y0 += sy
      end
    end
  end

  def draw_ascii(screen, connections, width = 60, height = 20)
    valid = screen.each_with_index.map { |(x, y), i| {idx: i, x: x, y: y} if !x.nan? }.compact
    return "No stars visible from this angle." if valid.empty?
    min_x = valid.map { |v| v[:x] }.min
    max_x = valid.map { |v| v[:x] }.max
    min_y = valid.map { |v| v[:y] }.min
    max_y = valid.map { |v| v[:y] }.max
    range_x = max_x - min_x
    range_y = max_y - min_y
    range_x = 1 if range_x == 0
    range_y = 1 if range_y == 0
    scale_x = (width - 4) / range_x
    scale_y = (height - 4) / range_y
    scale = [scale_x, scale_y].min
    grid = Array.new(height) { Array.new(width, " ") }
    to_grid = ->(x, y) {
      cx = ((x - min_x) * scale + 2).to_i
      cy = ((y - min_y) * scale + 2).to_i
      [cx, cy]
    }
    star_pos = {}
    valid.each do |v|
      cx, cy = to_grid.call(v[:x], v[:y])
      if cx >= 0 && cx < width && cy >= 0 && cy < height
        grid[cy][cx] = "*"
        star_pos[v[:idx]] = [cx, cy]
      end
    end
    connections.each do |i, j|
      if star_pos[i] && star_pos[j]
        x1, y1 = star_pos[i]
        x2, y2 = star_pos[j]
        draw_line(grid, x1, y1, x2, y2)
      end
    end
    grid.map(&:join).join("\n")
  end

  def view(name, az: nil, el: nil, scale: nil, proj: nil)
    @azimuth = az if az
    @elevation = el if el
    @scale = scale if scale
    @projection = proj if proj
    const = CONSTELLATIONS[name]
    return "Constellation '#{name}' not found." unless const
    points = const[:stars].map { |_, ra, dec, dist| ra_dec_to_cartesian(ra, dec, dist) }
    rotated = rotate(points)
    screen = project(rotated)
    ascii = draw_ascii(screen, const[:connections])
    header = "\n🌟 Constellation: #{name} (#{const[:abbr]})\n"
    header += "Azimuth: #{@azimuth}°  Elevation: #{@elevation}°  Scale: #{@scale}\n"
    header += "Projection: #{@projection}\n\n"
    header + ascii
  end

  def self.list
    CONSTELLATIONS.keys.map { |k| "#{k} (#{CONSTELLATIONS[k][:abbr]})" }.join("\n")
  end

  def self.info(name)
    const = CONSTELLATIONS[name]
    return "Constellation '#{name}' not found." unless const
    "\n✨ #{name} (#{const[:abbr]})\nMythology: #{const[:mythology]}\nNotable Objects: #{const[:notable_objects]}\n"
  end
end

options = {}
OptionParser.new do |opts|
  opts.banner = "Usage: constellation_3d.rb <command> [options]"
  opts.on("view", "View a constellation") { options[:cmd] = :view }
  opts.on("list", "List constellations") { options[:cmd] = :list }
  opts.on("info", "Show info") { options[:cmd] = :info }
  opts.on("--const CONST", "Constellation name") { |v| options[:const] = v }
  opts.on("--az DEG", Float, "Azimuth") { |v| options[:az] = v }
  opts.on("--el DEG", Float, "Elevation") { |v| options[:el] = v }
  opts.on("--scale SCALE", Float, "Scale") { |v| options[:scale] = v }
  opts.on("--projection PROJ", %w[ortho persp], "Projection") { |v| options[:proj] = v }
end.parse!

map = StarMap3D.new
case options[:cmd]
when :view
  if options[:const].nil?
    puts "view requires --const"
    exit 1
  end
  puts map.view(options[:const], az: options[:az], el: options[:el], scale: options[:scale], proj: options[:proj])
when :list
  puts "📋 Available Constellations:"
  puts StarMap3D.list
when :info
  if options[:const].nil?
    puts "info requires --const"
    exit 1
  end
  puts StarMap3D.info(options[:const])
else
  puts "Unknown command. Use view, list, info."
end
