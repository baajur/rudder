// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: 2019-2020 Normation SAS

use lazy_static::lazy_static;
use regex::Regex;
use std::fmt;

#[derive(Clone)]
pub struct Backtrace(backtrace::Backtrace);

impl Backtrace {
    pub fn new() -> Self {
        Self(backtrace::Backtrace::new())
    }

    /// this funtion can be invoked from anywhere in the code to get a proper backtrace up to the creation of the programs thread
    /// This could be done for debug purposes or placed at strategic places like panic calls
    fn format_symbol(index: usize, sym: &backtrace::BacktraceSymbol) -> Option<String> {
        lazy_static! {
            // if starts with rudderc + remove ending addr that is not helpful as is + handle aliases
            static ref SYMBOL_PATH: Regex = Regex::new(r"^<?(?P<path>rudderc(::[{}\d\w]+)+)( as .*>(?P<endingpath>::[{}\d\w]+)+)?(::[\da-z]+)$").unwrap();
        }

        Self::get_symbol_name(sym).and_then(|str_name| {
            SYMBOL_PATH
                .captures(&str_name)
                .and_then(|caps| match (caps.name("path"), caps.name("endingpath")) {
                    (None, _) => None,
                    (Some(start), Some(end)) => [start.as_str(), end.as_str()].concat().into(),
                    (Some(start), None) => start.as_str().to_owned().into(),
                })
                .and_then(|fmt_name| {
                    // do not put output related calls in the backtrace since it always ultimately calls panic_hook and print_backtrace
                    if fmt_name.starts_with("rudderc::output")
                        || fmt_name.starts_with("rudderc::error::Error::new")
                    {
                        return None;
                    }
                    Some(format!(
                        "  {offset}{name} at '{filename}:{line}'",
                        offset = " ".repeat(index * 2),
                        name = fmt_name,
                        filename = Self::get_symbol_filename(sym),
                        line = Self::get_symbol_line(sym)
                    ))
                })
        })
    }

    fn get_symbol_name(sym: &backtrace::BacktraceSymbol) -> Option<String> {
        sym.name().and_then(|name| format!("{:?}", name).into())
    }

    fn get_symbol_line(sym: &backtrace::BacktraceSymbol) -> String {
        sym.lineno()
            .and_then(|n| n.to_string().into())
            .unwrap_or("undefined".to_owned())
    }

    fn get_symbol_filename(sym: &backtrace::BacktraceSymbol) -> String {
        sym.filename()
            .and_then(|path| path.to_str())
            .unwrap_or("undefined")
            .to_owned()
    }

    // TODO backtrace as vec, to print it into a backtrace field in the json format
}

/// Display backtrace to the final user
impl fmt::Display for Backtrace {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        let stringified_trace = self
            .0
            .frames()
            .iter()
            .filter_map(|frame| {
                frame
                    .symbols()
                    .into_iter()
                    .enumerate()
                    .map(|(index, sym)| Self::format_symbol(index, sym))
                    .collect::<Option<Vec<String>>>()
            })
            .flatten()
            .collect::<Vec<String>>()
            .join("\n");

        write!(f, "\nTrace:\n{}", stringified_trace)
    }
}
