var printInteger : (integer): void;

function bar(x : integer, y : integer) : integer;
begin
  bar := x + y;
end

function foo(x : integer, y : integer) : integer;
begin
  foo := bar(y, x);
end

begin
    result := foo(3, 4);
    printInteger(result);
end