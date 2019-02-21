STDOUT.sync = true

def e(*var)
  STDERR.puts var
end

DIRECTIONS = ["UP", "RIGHT", "DOWN", "LEFT"]

MY_ID = gets.to_i
HEIGHT = gets.to_i
WIDTH = gets.to_i

$grid = []

loop do
  map = []
  HEIGHT.times do
    line = gets.chomp
    map << line
  end
  e(map)
  puts DIRECTIONS.sample
end
