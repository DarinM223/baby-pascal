var printInteger : (integer): void;

function fibonacci(n : integer) : integer;
begin
    if n <= 1 then
    begin
        fibonacci := n;
    end
    else
    begin
        fibonacci := fibonacci(n - 1) + fibonacci(n - 2);
    end
end

begin
    result := fibonacci(9);
    printInteger(result);
end