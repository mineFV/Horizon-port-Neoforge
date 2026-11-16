//! A stand-in for Horizon's `horizon_world::nbt` (crates/horizon-world/src/nbt.rs, MIT): the types the bridge
//! (BridgeRust) names, in the same shape, for the tests to compile and run the bridge without Horizon.
pub mod nbt {
    use std::collections::BTreeMap;

    #[derive(Clone, Debug, Default, PartialEq, Eq, PartialOrd, Ord, Hash)]
    pub struct JavaString(pub Vec<u16>);

    impl From<&str> for JavaString {
        fn from(value: &str) -> Self {
            Self(value.encode_utf16().collect())
        }
    }

    pub type Compound = BTreeMap<JavaString, Tag>;

    #[derive(Clone, Debug, PartialEq, Eq, Hash)]
    pub enum Tag {
        End,
        Byte(i8),
        Short(i16),
        Int(i32),
        Long(i64),
        Float(u32),
        Double(u64),
        ByteArray(Vec<u8>),
        String(JavaString),
        List(Vec<Tag>),
        Compound(Compound),
        IntArray(Vec<i32>),
        LongArray(Vec<i64>),
    }
}
