STDOUT.sync = true

def e(var)
  STDERR.puts var
end

DIRECTIONS = ["UP", "RIGHT", "DOWN", "LEFT"]

loop do
  # TODO: get input

  # e("ruby player")
  puts DIRECTIONS.sample
end
