-- Helper functions
local function disconnectNode(nodeKey)
    local prevKey = redis.call('HGET', KEYS[2], nodeKey)
    local nextKey = redis.call('HGET', KEYS[3], nodeKey)

    if prevKey then
        if nextKey then
            redis.call('HSET', KEYS[3], prevKey, nextKey)
        else
            redis.call('HDEL', KEYS[3], prevKey)
        end
    end

    if nextKey then
        if prevKey then
            redis.call('HSET', KEYS[2], nextKey, prevKey)
        else
            redis.call('HDEL', KEYS[2], nextKey)
        end
    end

    redis.call('HDEL', KEYS[2], nodeKey)
    redis.call('HDEL', KEYS[3], nodeKey)
end

-- Main flow
local key = ARGV[1]
local value = redis.call('HGET', KEYS[1], key)

if not value then
    return nil
end

local prevKey = redis.call('HGET', KEYS[2], key)
local currentWarmest = redis.call('HGET', KEYS[4], 'warmest')

if currentWarmest == key then
    if prevKey then
        redis.call('HSET', KEYS[4], 'warmest', prevKey)
    else
        redis.call('HDEL', KEYS[4], 'warmest')
    end
end

disconnectNode(key)
redis.call('HDEL', KEYS[1], key)

return value
