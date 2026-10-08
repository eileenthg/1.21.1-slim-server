"""Minimal gzip NBT reader/writer for Structurize .blueprint files.

Values are wrapped as (tag_id, value) so a round trip keeps exact tag types.
"""
import gzip
import io
import struct

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY = range(13)


def _read_str(f):
    (n,) = struct.unpack(">H", f.read(2))
    return f.read(n).decode("utf-8", "surrogatepass")


def _read_payload(f, t):
    if t == BYTE:
        return struct.unpack(">b", f.read(1))[0]
    if t == SHORT:
        return struct.unpack(">h", f.read(2))[0]
    if t == INT:
        return struct.unpack(">i", f.read(4))[0]
    if t == LONG:
        return struct.unpack(">q", f.read(8))[0]
    if t == FLOAT:
        return struct.unpack(">f", f.read(4))[0]
    if t == DOUBLE:
        return struct.unpack(">d", f.read(8))[0]
    if t == BYTE_ARRAY:
        (n,) = struct.unpack(">i", f.read(4))
        return bytearray(f.read(n))
    if t == STRING:
        return _read_str(f)
    if t == LIST:
        et = f.read(1)[0]
        (n,) = struct.unpack(">i", f.read(4))
        return (et, [_read_payload(f, et) for _ in range(n)])
    if t == COMPOUND:
        d = {}
        while True:
            ct = f.read(1)[0]
            if ct == END:
                return d
            name = _read_str(f)
            d[name] = (ct, _read_payload(f, ct))
    if t == INT_ARRAY:
        (n,) = struct.unpack(">i", f.read(4))
        return list(struct.unpack(f">{n}i", f.read(4 * n)))
    if t == LONG_ARRAY:
        (n,) = struct.unpack(">i", f.read(4))
        return list(struct.unpack(f">{n}q", f.read(8 * n)))
    raise ValueError(f"bad tag {t}")


def _write_str(f, s):
    b = s.encode("utf-8", "surrogatepass")
    f.write(struct.pack(">H", len(b)))
    f.write(b)


def _write_payload(f, t, v):
    if t == BYTE:
        f.write(struct.pack(">b", v))
    elif t == SHORT:
        f.write(struct.pack(">h", v))
    elif t == INT:
        f.write(struct.pack(">i", v))
    elif t == LONG:
        f.write(struct.pack(">q", v))
    elif t == FLOAT:
        f.write(struct.pack(">f", v))
    elif t == DOUBLE:
        f.write(struct.pack(">d", v))
    elif t == BYTE_ARRAY:
        f.write(struct.pack(">i", len(v)))
        f.write(bytes(v))
    elif t == STRING:
        _write_str(f, v)
    elif t == LIST:
        et, items = v
        f.write(bytes([et]))
        f.write(struct.pack(">i", len(items)))
        for it in items:
            _write_payload(f, et, it)
    elif t == COMPOUND:
        for name, (ct, cv) in v.items():
            f.write(bytes([ct]))
            _write_str(f, name)
            _write_payload(f, ct, cv)
        f.write(bytes([END]))
    elif t == INT_ARRAY:
        f.write(struct.pack(">i", len(v)))
        f.write(struct.pack(f">{len(v)}i", *v))
    elif t == LONG_ARRAY:
        f.write(struct.pack(">i", len(v)))
        f.write(struct.pack(f">{len(v)}q", *v))
    else:
        raise ValueError(f"bad tag {t}")


def load(data: bytes):
    f = io.BytesIO(gzip.decompress(data))
    t = f.read(1)[0]
    name = _read_str(f)
    return name, _read_payload(f, t)


def dump(name, root) -> bytes:
    f = io.BytesIO()
    f.write(bytes([COMPOUND]))
    _write_str(f, name)
    _write_payload(f, COMPOUND, root)
    return gzip.compress(f.getvalue())
